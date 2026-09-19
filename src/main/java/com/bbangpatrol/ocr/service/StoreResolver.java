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
 * 빵집은 100곳 남짓이라 전부 읽어 메모리에서 맞춘다. 쿼리를 나누는 것보다 단순하고 빠르다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StoreResolver {

    private final BakeryRepository bakeryRepository;
    private final ReceiptStoreMatcher storeMatcher;

    /**
     * 판별 결과.
     * - matched: 한 곳으로 확정됐을 때
     * - ambiguous: 후보가 둘 이상이라 사용자에게 물어야 할 때
     * - notRegistered: 우리 서비스에 없는 가게
     */
    public record Resolution(Bakery matched, List<Bakery> candidates) {

        public static Resolution of(Bakery bakery) {
            return new Resolution(bakery, List.of(bakery));
        }

        public static Resolution ambiguous(List<Bakery> candidates) {
            return new Resolution(null, candidates);
        }

        public static Resolution notRegistered() {
            return new Resolution(null, List.of());
        }

        public boolean isMatched() {
            return matched != null;
        }

        public boolean isAmbiguous() {
            return matched == null && candidates.size() > 1;
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
                // 한 법인번호를 여러 지점이 쓰는 브랜드가 있다. 등록된 지점의 주소와 명백히 다르면
                // 그 지점은 우리 서비스에 없는 것이므로 확정하지 않는다 (가게를 고르는 흐름의 판단과 같다)
                if (storeMatcher.matchAddress(found.getAddress(), parsed.address())
                        == ReceiptStoreMatcher.AddressMatch.MISMATCH) {
                    log.info("[RESOLVE] 사업자번호는 맞지만 등록된 지점과 주소가 다르다. bakeryId={}", found.getId());
                    return Resolution.notRegistered();
                }
                return Resolution.of(found);
            }
            if (byNumber.size() > 1) {
                // 같은 번호가 여러 행에 있는 경우. 주소로 좁혀 본다
                List<Bakery> narrowed = byAddress(byNumber, parsed);
                return narrowed.size() == 1 ? Resolution.of(narrowed.get(0)) : Resolution.ambiguous(byNumber);
            }
        }

        // 2. 상호 + 주소 (사업자번호를 모르는 가게)
        List<Bakery> byName = bakeries.stream()
                .filter(b -> storeMatcher.matchesName(b.getName(), parsed.bakeryName()))
                .toList();
        List<Bakery> byNameAndAddress = byAddress(byName, parsed);

        if (byNameAndAddress.size() == 1) {
            return Resolution.of(byNameAndAddress.get(0));
        }
        if (byNameAndAddress.size() > 1) {
            return Resolution.ambiguous(byNameAndAddress);
        }

        log.info("[RESOLVE] 등록되지 않은 가게. 영수증 상호={}, 사업자번호={}",
                parsed.bakeryName(), parsed.businessNumber());
        return Resolution.notRegistered();
    }

    // 주소가 확실히 맞는 곳만 남긴다. UNKNOWN(주소를 못 읽음)은 남기지 않는다 —
    // 가게를 고르지 않는 흐름에서는 주소가 유일한 구분 수단이다
    private List<Bakery> byAddress(List<Bakery> bakeries, ReceiptParseResult parsed) {
        return bakeries.stream()
                .filter(b -> storeMatcher.matchAddress(b.getAddress(), parsed.address())
                        == ReceiptStoreMatcher.AddressMatch.MATCH)
                .toList();
    }

    private String digits(String value) {
        if (value == null || value.isBlank()) return null;
        String only = value.replaceAll("[^0-9]", "");
        return only.isEmpty() ? null : only;
    }
}
