package com.len.config;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;

/**
 * @author zhuxiaomeng
 * @date 2018/1/2.
 * @email 154040976@qq.com
 */
@Configuration
@EnableTransactionManagement
public class DruidConfig {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.filters}")
	private String filters; // Druid内置提供一个StatFilter，用于统计监控信息,StatFilter的别名是stat
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    @Value("${spring.datasource.initialSize}")
    private int initialSize;
    @Value("${spring.datasource.minIdle}")
    private int minIdle;

    @Bean
    @Primary
    public DataSource getDataSource() {
        DruidDataSource datasource = new DruidDataSource();

        datasource.setUrl(url);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setDriverClassName(driverClassName);
        datasource.setInitialSize(initialSize);
        datasource.setMinIdle(minIdle);
		try {
			datasource.setFilters(filters); // 打开Druid的监控统计功能
			// 置防御SQL注入攻击
			// datasource.setFilters("wall");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return datasource;
    }

	// WebStatFilter用于采集web-jdbc关联监控的数据。
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico,/druid/*,*.html");
		filterRegistrationBean.addInitParameter("sessionStatEnable", "true"); // 开启或关闭session统计功能
		// 默认是1000
		// filterRegistrationBean.addInitParameter("sessionStatMaxCount", "1000");
		// 配置抓取session的用户名. XXX session名
		// filterRegistrationBean.addInitParameter("principalSessionName", "XXX.user");
		// 配置抓取Cookie的用户名. XXX Cookie名
		// filterRegistrationBean.addInitParameter("principalCookieName", "XXX.user");
		// 配置profileEnable能够监控单个url调用的sql列表。
		filterRegistrationBean.addInitParameter("profileEnable", "true");

        DelegatingFilterProxy proxy = new DelegatingFilterProxy();
        proxy.setTargetFilterLifecycle(true);
        proxy.setTargetBeanName("shiroFilter");

        filterRegistrationBean.setFilter(proxy);
        return filterRegistrationBean;
    }

	// 怎样使用Druid的内置监控页面
	// Druid内置提供了一个StatViewServlet用于展示Druid的统计信息
    @Bean
    public ServletRegistrationBean druidServlet() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        servletRegistrationBean.setServlet(new StatViewServlet());
		servletRegistrationBean.addUrlMappings("/druid/*"); // druid/index.html
        Map<String, String> initParameters = new HashMap<String, String>();
		initParameters.put("resetEnable", "false"); // 允许清空统计数据
		initParameters.put("loginUsername", "druid"); // 用户名
		initParameters.put("loginPassword", "druid");
		/*
		 * deny优先于allow，如果在deny列表中，就算在allow列表中，也会被拒绝。 如果allow没有配置或者为空，则允许所有访问
		 */
		initParameters.put("allow", ""); // deny 允许或拒绝那些ip访问，多个ip用逗号分隔
        servletRegistrationBean.setInitParameters(initParameters);
        return servletRegistrationBean;
    }

	// Druid提供了Spring和Jdbc的关联监控。
	// com.alibaba.druid.support.spring.stat.DruidStatInterceptor是一个标准的Spring
	// MethodInterceptor。可以灵活进行AOP配置。
	@Bean
	public DruidStatInterceptor getDruidStatInterceptor() {
		return new DruidStatInterceptor();
	}

	@Bean
	@Scope("prototype")
	public JdkRegexpMethodPointcut getJdkRegexpMethodPointcut() {
		JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
		String[] str = { "com.len.service.*", "com.len.mapper.*" };
		pointcut.setPatterns(str);
		return pointcut;
	}
}
