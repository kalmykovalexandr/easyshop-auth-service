//package com.easyshop.auth.web;
//
//import com.easyshop.auth.model.RegistrationResult;
//import com.easyshop.auth.security.VerificationRateLimiter;
//import com.easyshop.auth.service.impl.AuthService;
//import org.hamcrest.Matchers;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@ExtendWith(MockitoExtension.class)
//class AuthControllerTest {
//
//    @Mock
//    private AuthService authService;
//
//    @Mock
//    private VerificationRateLimiter rateLimiter;
//
//    private MockMvc mockMvc;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders
//                .standaloneSetup(new AuthController(authService, rateLimiter))
//                .setMessageConverters(new MappingJackson2HttpMessageConverter())
//                .build();
//    }
//
//    @Test
//    void healthEndpointWorks() throws Exception {
//        mockMvc.perform(get("/healthz"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.ok").value(true));
//    }
//
//    @Test
//    void readyEndpointWorks() throws Exception {
//        mockMvc.perform(get("/readyz"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.ok").value(true));
//    }
//
//    @Test
//    void registerSuccessfully() throws Exception {
//        //hen(authService.register(any())).thenReturn(RegistrationResult.successful());
//        when(authService.getRegistrationSuccessMessage()).thenReturn("Registration successful. Check your e-mail to activate your account.");
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"email\":\"user@example.com\",\"password\":\"S3cure@123\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.ok").value(true))
//                .andExpect(jsonPath("$.message").value("Registration successful. Check your e-mail to activate your account."));
//    }
//
//    @Test
//    void registerReturnsErrorDetails() throws Exception {
//        //when(authService.register(any())).thenReturn(RegistrationResult.failure(false, true));
//        when(authService.getPasswordValidationMessage()).thenReturn("Password requirements");
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"email\":\"user@example.com\",\"password\":\"S3cure@123\"}"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.ok").value(false))
//                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Password requirements")));
//    }
//}
