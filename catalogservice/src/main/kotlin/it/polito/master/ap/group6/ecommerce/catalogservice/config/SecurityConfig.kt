//======================================================================================================================
//   Libraries
//======================================================================================================================

//------- package declaration --------------------------------------------------
package it.polito.master.ap.group6.ecommerce.catalogservice.config

//------- external dependencies ------------------------------------------------
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter



//======================================================================================================================
//   Class
//======================================================================================================================
/**
 * Configure security and login credentials.
 *
 * @author Nicolò Chiapello
 */
@EnableWebSecurity()
@Configuration
class SecurityConfig: WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        // force auth for every method
        http.httpBasic() //select the type of authentication. This is the basic, there are others for microservices
            .and() //specify the rules
            .authorizeRequests()
                // permit all methods on Warehouse microservice
            .antMatchers("/catalog/products/**").authenticated()
                // permit all methods on Order microservice
            .antMatchers("/catalog/orders/**").authenticated()
                // permit all methods on Wallet microservice
            .antMatchers("/catalog/wallet/**").authenticated()
            .anyRequest().authenticated()

        // permit all (alternative for testing)
        //http.authorizeRequests().anyRequest().permitAll()
    }

    // plug your UserDetailsService
    // https://prog.world/rest-api-authentication-with-spring-security-and-mongodb/
}

@Configuration
@EnableGlobalMethodSecurity(
    jsr250Enabled = true, //specify the RolesAllowed annotation is active
    prePostEnabled = true, //specify the prepost is enabled. This allows to execute a method and decide if the operation is authorized or not
    securedEnabled = true, //Not used
)
class MethodSecurityConfig: GlobalMethodSecurityConfiguration()