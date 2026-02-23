package dukku.common.shared.auth.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public final class AuthApiDocs {

    private AuthApiDocs() {
    }

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "Auth API",
            description = "Authentication, token refresh, logout, and social login APIs."
    )
    public @interface AuthTag {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "User Login",
            description = "Validates email/password and issues access/refresh tokens. " +
                    "This endpoint only allows Role.USER accounts. Role.ADMIN receives 401.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "semicolon@example.com",
                                              "password": "Password123!"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login success"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized (invalid credentials or role mismatch)")
            }
    )
    public @interface Login {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "Admin Login",
            description = "Admin-only login endpoint. " +
                    "This endpoint only allows Role.ADMIN accounts. Role.USER receives 401.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "admin@semicolon.com",
                                              "password": "Admin123!"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Admin login success"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized (invalid credentials or role mismatch)")
            }
    )
    public @interface AdminLogin {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "Start Google Social Login",
            description = "Redirects to the Google OAuth authorization page.",
            responses = {
                    @ApiResponse(responseCode = "302", description = "Redirect to Google OAuth"),
                    @ApiResponse(responseCode = "500", description = "Failed to build redirect URL")
            }
    )
    public @interface StartGoogleSocialLogin {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "Refresh Token",
            description = "Reissues access/refresh tokens using `X-Refresh-Token` header.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Refresh success"),
                    @ApiResponse(responseCode = "401", description = "Invalid refresh token")
            }
    )
    public @interface Refresh {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "Logout",
            description = "Invalidates server-side refresh token using `X-Refresh-Token` header.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Logout success")
            }
    )
    public @interface Logout {
    }
}
