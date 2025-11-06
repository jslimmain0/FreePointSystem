package me.jslim.point.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jslim.point.application.dto.*;
import me.jslim.point.application.service.PointCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;



@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {
    private final PointCommandService pointCommandService;
    /**
     * 포인트 적립
     */
    @PostMapping("/earn")
    public ResponseEntity<EarnResult> earnPoints(@Valid @RequestBody EarnCmd request) {
        LocalDate earnDate = LocalDate.now();
        return ResponseEntity.ok(pointCommandService.earn(request, earnDate));
    }

    /**
     * 포인트 사용
     */
    @PostMapping("/use")
    public ResponseEntity<UseResult> earnCancel(@Valid @RequestBody UseCmd request) {
        LocalDate useDate = LocalDate.now();
        return  ResponseEntity.ok(pointCommandService.use(request, useDate));
    }

    /**
     * 포인트 적립 취소
     */
    @PostMapping("/earn/cancel")
    public ResponseEntity<EarnCancelResult> earnCancel(@Valid @RequestBody EarnCancelCmd request) {
        return ResponseEntity.ok(pointCommandService.cancelEarn(request));
    }


    /**
     * 포인트 사용 취소
     */
    @PostMapping("/use/cancel")
    public ResponseEntity<UseCancelResult> useCancel(@Valid @RequestBody UseCancelCmd request) {
        return ResponseEntity.ok(pointCommandService.cancelUse(request));
    }
}
