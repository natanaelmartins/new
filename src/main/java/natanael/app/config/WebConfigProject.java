package natanael.app.config;

// Import necessary classes for configuration and security
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import natanael.app.services.UsuarioUserDetailsService;

// This annotation indicates that this class provides Spring configuration
@Configuration
// This annotation enables Spring Security for the application
@EnableWebSecurity
// This annotation enables Spring MVC for the application
@EnableWebMvc
public class WebConfigProject implements WebMvcConfigurer {

    // Autowire the UserDetailsService to manage user authentication
    @Autowired
    UsuarioUserDetailsService usuarioUserDetailsService;

    // This method creates a BCryptPasswordEncoder bean for password encoding
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This method configures the security filter chain for the application
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // Disable CSRF protection
                .csrf(AbstractHttpConfigurer::disable)
                // Configure authorization for HTTP requests
                .authorizeHttpRequests(registry -> {
                    // Allow access to specific endpoints without authentication
                    registry.requestMatchers("/img/**", "/cadastro", "/salvarCadastro").permitAll();
                    // Require authentication for all other requests
                    registry.anyRequest().authenticated();
                })
                // Configure form login settings
                .formLogin(httpSecurityFormLoginConfigurer -> {
                    httpSecurityFormLoginConfigurer
                            .loginPage("/login") // Set the login page URL
                            .successHandler(new AuthenticationSuccessHandler()) // Set the success handler after login
                            .permitAll(); // Allow everyone to access the login page
                })
                .build(); // Build the security filter chain
    }

    // This method creates an AuthenticationProvider bean for user authentication
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioUserDetailsService); // Set the user details service
        provider.setPasswordEncoder(passwordEncoder()); // Set the password encoder
        return provider; // Return the configured provider
    }

    // This method adds resource handlers for serving static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map all requests to static resources in the classpath
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/"); // Specify the location of static resources
    }
}
