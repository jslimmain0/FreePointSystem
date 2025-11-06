package me.jslim.point;

import lombok.extern.slf4j.Slf4j;
import me.jslim.point.application.dto.*;
import me.jslim.point.application.service.PointCommandService;
import me.jslim.point.application.service.PointExpireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@DisplayName("예시")
class ExampleTest {
    @Autowired
    PointCommandService pointCommandService;

    @Autowired
    PointExpireService pointExpireService;

    @Autowired
    JdbcTemplate jdbc;

    String userId = "jslim";
    LocalDate date = LocalDate.now();

    @BeforeEach
    void resetDb() {
        jdbc.batchUpdate("""
            SET REFERENTIAL_INTEGRITY FALSE;
            TRUNCATE TABLE POINT;
            TRUNCATE TABLE POINT_EARN;
            TRUNCATE TABLE POINT_EARN_CANCEL;
            TRUNCATE TABLE POINT_USE;
            TRUNCATE TABLE POINT_USE_CANCEL;
            TRUNCATE TABLE POINT_WALLET;
            SET REFERENTIAL_INTEGRITY TRUE;
        """.split(";"));
    }


    /**
     * <pre>
     *  1. 1000원 적립한다 (총 잔액 0 -> 1000 원)
     *      1. pointKey : A 로 할당
     *  2. 500원 적립한다 (총 잔액 1000 -> 1500 원)
     *      1. pointKey : B 로 할당
     *  3. 주문번호 A1234 에서 1200원 사용한다 (총 잔액 1500 -> 300 원)
     *      1. pointKey : C 로 할당
     *      2. A 적립에서 1000원 사용함 -> A의 사용가능 잔액은 1000 -> 0
     *      3. B 적립에서 200원 사용함 -> B의 사용가능 잔액은 500 -> 300
     *  4. A의 적립이 만료되었다
     *  5. C의 사용금액 1200원 중 1100원을 부분 사용취소 한다 (총 잔액 300 -> 1400 원)
     *      1. pointKey : D 로 할당
     *      2. 1200원은 A와 B에서 사용되었다.
     *      3. 그래서, 사용취소 하면 A의 사용가능 잔액이 0 -> 1000원 되어야 하지만, A는 이미 만료일이 지났기 때문에 pointKey E 로 1000원이 신규적립 되어야 한다.
     *      4. B는 만료되지 않았기 때문에 사용가능 잔액은 300 -> 400원이 된다.
     *      5. C는 이제 1200원 사용금액중 100원을 부분취소 할 수 있다.
     *  </pre>
     */


    @Test
    @DisplayName("예시 테스트")
    void earnPoints() {
        // 1. 1000원 적립한다 (총 잔액 0 -> 1000 원)
        index1();
        printPoint();

        // 22. 500원 적립한다 (총 잔액 1000 -> 1500 원)
        index2();
        printPoint();

        // 3. 주문번호 A1234 에서 1200원 사용한다 (총 잔액 1500 -> 300 원)
        String pointKeyC = index3();
        printPoint();

        // 4. A의 적립이 만료되었다
        index4();
        printPoint();

        // 5. C의 사용금액 1200원 중 1100원을 부분 사용취소 한다 (총 잔액 300 -> 1400 원)
        index5(pointKeyC);
        printPoint();
    }

    void index1() {
        // given
        String expireDate = date.plusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE);
        EarnCmd earnCmd = new EarnCmd(userId, 1_000L, null, expireDate);

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, date);

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(1_000);
    }

    void index2() {
        // given
        EarnCmd earnCmd = new EarnCmd(userId, 500L, null, null);

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, date);

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(1_500);
    }

    String index3() {
        // given
        String orderNo = "A1234";
        UseCmd useCmd = new UseCmd(userId, 1200L, orderNo);

        // when
        UseResult useResult = pointCommandService.use(useCmd, date);

        // then
        assertThat(useResult.balanceAmount()).isEqualTo(300);
        return useResult.pointKey();
    }

    void index4(){
        pointExpireService.findActiveExpiring(date.plusDays(6)); // A 적립을 만료시키기 위해 6일 이 지난 걸로 설정
    }

    void index5(String cancelPointKey){
        // given
        UseCancelCmd cancelCmd = new UseCancelCmd(userId, cancelPointKey, 1100L);

        // when
        UseCancelResult useCancelResult = pointCommandService.cancelUse(cancelCmd);

        // then
        assertThat(useCancelResult.balanceAmount()).isEqualTo(1400);
    }


    void printPoint() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT P.POINT_KEY AS A
                 , (CASE
                     WHEN P.POINT_TYPE = 'EARN' THEN '[  적립  ]'
                     WHEN P.POINT_TYPE = 'EARN_CANCEL' THEN '[적립 취소]'
                     WHEN P.POINT_TYPE = 'USE' THEN '[  사용  ]'
                     ELSE '[사용 취소]'
                   END) AS B
                 , P.POINT_AMOUNT AS C
                 , (CASE
                     WHEN E.EARN_STATUS = 'EXPIRED' THEN '(만료됨)'
                     ELSE ''
                   END) AS D
                 , (CASE
                     WHEN E.EARN_TYPE = 'EARN_AS_USE_CANCEL' THEN '(사용취소로 인한 적립)'
                     ELSE ''
                   END) AS E
              FROM POINT P
              LEFT JOIN POINT_EARN AS E ON P.POINT_KEY = E.POINT_KEY
        """);

        log.info("포인트내역");
        log.info("---------------------------------------------------------------");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            char alp = (char)('A' + i);
            String pointKey = r.get("A").toString();
            log.info("{} ({}) | {} {}원 {}{}",
                    "POINT_KEY "+alp,
                    String.format("%-12s", pointKey.substring(0, 6) + "..." + pointKey.substring(pointKey.length() - 4)),
                    String.format("%-8s", r.get("B")),
                    String.format("%6s", r.get("C")),
                    r.get("D"), r.get("E"));
        }
        log.info("---------------------------------------------------------------");
    }
}