package com.bbangpatrol.ocr.service;

import com.bbangpatrol.bakery.entity.Bakery;
import com.bbangpatrol.bakery.repository.BakeryRepository;
import com.bbangpatrol.ocr.dto.ReceiptParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 영수증만 보고 어느 빵집인지 찾아낸다. 사용자가 가게를 고르지 않는 흐름에서 쓴다.
 *
 * 가게를 고르는 흐름(기존 엔드포인트)은 "사용자가 고른 가게 ↔ 영수증"을 대조했지만,
 * 여기서는 대조할 기준이 없으므로 영수증이 유일한 근거다. 그래서 순서를 둔다.
 *   1. 사업자번호 정확 일치 — 번호를 아는 가게(88곳)는 여기서 끝난다
 *   2. 번호로 못 찾으면 상호 + 주소가 모두 맞는 곳을 찾는다 (번호를 모르는 가게용)
 *   3. 후보가 둘 이상이면 고르지 않는다. 틀린 가게에 방문을 붙이는 것보다 사용자에게 묻는 편이 낫다
 *
 * 못 찾은 이유를 {@link Reason} 으로 구분한다. "등록되지 않은 가게"와 "다른 지점"과
 * "주소를 못 읽음"은 사용자가 할 수 있는 일이 서로 달라서, 같은 안내로 뭉뚱그리면 오해를 준다.
 *
 * 빵집은 100곳 남짓이라 전부 읽어 메모리에서 맞춘다. 쿼리를 나누는 것보다 단순하고 빠르다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StoreResolver {

    private final BakeryRepository bakeryRepository;
    private final ReceiptStoreMatcher storeMatcher;

    public enum Reason {
        /** 한 곳으로 확정 */
        MATCHED,
        /** 등록된 가게이긴 한데 주소가 달라 다른 지점으로 보인다 */
        BRANCH_MISMATCH,
        /** 후보가 둘 이상이라 고르지 못했다 */
        AMBIGUOUS,
        /** 주소를 못 읽어 가게를 좁히지 못했다 (재촬영하면 된다) */
        ADDRESS_UNREADABLE,
        /** 우리 서비스에 없는 가게 */
        NOT_REGISTERED
    }

    public record Resolution(Reason reason, Bakery matched, List<Bakery> candidates) {

        public static Resolution of(Bakery bakery) {
            return new Resolution(Reason.MATCHED, bakery, List.of(bakery));
        }

        public static Resolution failed(Reason reason, List<Bakery> candidates) {
            return new Resolution(reason, null, candidates);
        }

        public boolean isMatched() {
            return reason == Reason.MATCHED;
        }
    }

    public Resolution resolve(ReceiptParseResult parsed) {
        List<Bakery> bakeries = bakeryRepository.findAllByDeletedAtIsNull();

        // 1. 사업자번호
        String receiptNumber = digits(parsed.businessNumber());
        if (receiptNumber != null) {
            List<Bakery> byNumber = bakeries.stream()
                    .filter(b -> receiptNumber.equals(digits(b.getBusinessNumber())))
                    .toList();

            if (byNumber.size() == 1) {
                Bakery found = byNumber.get(0);
                // 한 법인번호를 여러 지점이 쓰는 브랜드가 있다. 등록된 지점과 주소가 명백히 다르면
                // 그 지점은 우리 서비스에 없는 것이므로 확정하지 않는다 (가게를 고르는 흐름의 판단과 같다)
                if (addressOf(found, parsed) == ReceiptStoreMatcher.AddressMatch.MISMATCH) {
                    log.info("[RESOLVE] 사업자번호는 맞지만 등록된 지점과 주소가 다르다. bakeryId={}", found.getId());
                    return Resolution.failed(Reason.BRANCH_MISMATCH, byNumber);
                }
                return Resolution.of(found);
            }
            if (byNumber.size() > 1) {
                // 같은 번호가 여러 행에 있는 경우. 주소로 좁혀 본다
                List<Bakery> narrowed = matchingAddress(byNumber, parsed);
                if (narrowed.size() == 1) {
                    return Resolution.of(narrowed.get(0));
                }
                return Resolution.failed(Reason.AMBIGUOUS, byNumber);
            }
        }

        // 2. 상호 + 주소 (사업자번호를 모르는 가게)
        List<Bakery> byName = bakeries.stream()
                .filter(b -> storeMatcher.matchesName(b.getName(), parsed.bakeryName()))
                .toList();

        if (byName.isEmpty()) {
            log.info("[RESOLVE] 등록되지 않은 가게. 영수증 상호={}, 사업자번호={}",
                    parsed.bakeryName(), parsed.businessNumber());
            return Resolution.failed(Reason.NOT_REGISTERED, List.of());
        }

        List<Bakery> narrowed = matchingAddress(byName, parsed);
        if (narrowed.size() == 1) {
            return Resolution.of(narrowed.get(0));
        }
        if (narrowed.size() > 1) {
            return Resolution.failed(Reason.AMBIGUOUS, narrowed);
        }

        // 상호는 맞는데 주소로 좁히지 못했다. 주소를 못 읽은 것과 주소가 다른 것은 사용자가 할 일이 다르다
        boolean addressUnreadable = byName.stream()
                .allMatch(b -> addressOf(b, parsed) == ReceiptStoreMatcher.AddressMatch.UNKNOWN);

        if (addressUnreadable) {
            log.info("[RESOLVE] 상호는 찾았지만 주소를 읽지 못해 특정 불가. 상호={}", parsed.bakeryName());
            return Resolution.failed(Reason.ADDRESS_UNREADABLE, byName);
        }

        log.info("[RESOLVE] 상호는 같지만 주소가 달라 다른 지점으로 보인다. 상호={}", parsed.bakeryName());
        return Resolution.failed(Reason.BRANCH_MISMATCH, byName);
    }

    // 주소가 확실히 맞는 곳만 남긴다. UNKNOWN(주소를 못 읽음)은 남기지 않는다 —
    // 가게를 고르지 않는 흐름에서는 주소가 유일한 구분 수단이다
    private List<Bakery> matchingAddress(List<Bakery> bakeries, ReceiptParseResult parsed) {
        return bakeries.stream()
                .filter(b -> addressOf(b, parsed) == ReceiptStoreMatcher.AddressMatch.MATCH)
                .toList();
    }

    private ReceiptStoreMatcher.AddressMatch addressOf(Bakery bakery, ReceiptParseResult parsed) {
        return storeMatcher.matchAddress(bakery.getAddress(), parsed.address());
    }

    private String digits(String value) {
        if (value == null || value.isBlank()) return null;
        String only = value.replaceAll("[^0-9]", "");
        return only.isEmpty() ? null : only;
    }
}
