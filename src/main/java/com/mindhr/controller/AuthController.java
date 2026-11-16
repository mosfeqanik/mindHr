package com.mindhr.controller;

import com.mindhr.dto.LoginRequest;
import com.mindhr.dto.LoginResponse;
import com.mindhr.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** QA accounts seeded by scripts/api_e2e_test.py; see docs/QA_CREDENTIALS.md. Test data only. */
    @Operation(summary = "Sign in and get a JWT",
            description = "Pick a QA login from the Examples list (password QaTest@123), press Execute, then click "
                    + "Authorize and paste `accessToken`. The Super Admin login is in backend/.env.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = {
                    @ExampleObject(name = "QA owner", summary = "ORG_OWNER of QA Test Org",
                            value = "{\"email\": \"qa.owner@mindhr.test\", \"password\": \"QaTest@123\"}"),
                    @ExampleObject(name = "QA team lead", summary = "TEAM_LEAD: approves emp1 and emp2",
                            value = "{\"email\": \"qa.lead@mindhr.test\", \"password\": \"QaTest@123\"}"),
                    @ExampleObject(name = "QA employee 1", summary = "EMPLOYEE (SELF scope)",
                            value = "{\"email\": \"qa.emp1@mindhr.test\", \"password\": \"QaTest@123\"}"),
                    @ExampleObject(name = "QA employee 2", summary = "EMPLOYEE (SELF scope)",
                            value = "{\"email\": \"qa.emp2@mindhr.test\", \"password\": \"QaTest@123\"}")
            }))
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
