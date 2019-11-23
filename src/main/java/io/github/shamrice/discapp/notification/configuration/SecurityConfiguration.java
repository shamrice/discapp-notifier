package io.github.shamrice.discapp.notification.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .headers()
                    .frameOptions().disable()
                    .and()
                .csrf()
                    .disable()
                .authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .logout()
                    .logoutUrl("/logout")
                    .deleteCookies("JSESSIONID")
                    .permitAll();
    }
}
