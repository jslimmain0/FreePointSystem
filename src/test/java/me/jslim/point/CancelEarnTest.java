package me.jslim.point;


import me.jslim.point.application.dto.*;
import me.jslim.point.application.service.PointCommandService;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("적립 취소")
public class CancelEarnTest {
    @Autowired
    PointCommandService pointCommandService;

    @Autowired
    JdbcTemplate jdbc;

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

    @Test
    @DisplayName("적립한 금액만큼 취소 가능")
    void earnCancel1() {
        // given
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 1000L, null, null);

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, earnDate);
        EarnCancelResult cancelResult = pointCommandService.cancelEarn(new EarnCancelCmd(userId, earnResult.pointKey()));

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(1000L);
        assertThat(cancelResult.balanceAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("적립한 금액중 일부가 사용된 경우라면 적립 취소 될 수 없다.")
    void earnCancel2() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 1000L, null, null);
        UseCmd useCmd1 = new UseCmd(userId, 111L, "ABC123");


        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> pointCommandService.cancelEarn(new EarnCancelCmd(userId, earnResult.pointKey())));


        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(1000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(889L);
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.EARN_USED_ERROR); // 최대 만료일 에러
    }
}
