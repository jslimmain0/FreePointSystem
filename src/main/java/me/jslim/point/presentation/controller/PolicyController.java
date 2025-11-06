package me.jslim.point.presentation.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jslim.point.application.support.PointPolicy;
import me.jslim.point.presentation.dto.PolicyUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policy")
@RequiredArgsConstructor
public class PolicyController {
    private final PointPolicy pointPolicy;

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@Valid @RequestBody PolicyUpdateRequest req) {
        pointPolicy.update(req.key(), req.value());
        pointPolicy.reload();
    }
}
