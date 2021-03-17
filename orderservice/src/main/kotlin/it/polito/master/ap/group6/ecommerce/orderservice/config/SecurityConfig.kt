package it.polito.master.ap.group6.ecommerce.orderservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod


/*@EnableWebSecurity()
@Configuration
class SecurityConfig: WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        // force auth for every method
        //http.httpBasic() //select the type of authentication. This is the basic, there are others for microservices
            //.and() //specify the rules
            //.authorizeRequests()
            // permit all methods
            //.antMatchers("/api/products/open/**").permitAll() //here we specify that all can access the opena api
            // permit GET only
            //.antMatchers(HttpMethod.GET,"/api/products/open/**").permitAll() //here we specify that only the get is allowed in the open api
            //.anyRequest().authenticated();

        //permit all
        http.authorizeRequests().anyRequest().authenticated();
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
class MethodSecurityConfig: GlobalMethodSecurityConfiguration()*/