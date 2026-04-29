package com.smart_desk.demo;

import com.smart_desk.demo.dto.UserDto;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "List users",
            description = "Returns users sorted by fullName ascending. Intended for assignee pickers.")
    @ApiResponse(responseCode = "200", description = "User list",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UserDto.UserSummary.class))))
    public List<UserDto.UserSummary> list(
            @Parameter(description = "Filter by role", schema = @Schema(implementation = User.Role.class))
            @RequestParam(required = false) User.Role role,

            @Parameter(description = "Include disabled accounts when false")
            @RequestParam(defaultValue = "true") boolean active) {

        return userService.search(role, active);
    }
}
