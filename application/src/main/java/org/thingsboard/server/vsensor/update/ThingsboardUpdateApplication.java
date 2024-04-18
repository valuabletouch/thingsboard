/*
* Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.vsensor.update;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.thingsboard.server.vsensor.update.service.ThingsboardUpdateService;

import java.util.Arrays;

@Slf4j
@ComponentScan({"org.thingsboard.server.vsensor.update",
        "org.thingsboard.server.install",
        "org.thingsboard.server.service.component",
        "org.thingsboard.server.service.install",
        "org.thingsboard.server.service.security.auth.jwt.settings",
        "org.thingsboard.server.dao",
        "org.thingsboard.server.common.stats",
        "org.thingsboard.server.common.transport.config.ssl",
        "org.thingsboard.server.cache",
        "org.thingsboard.server.springfox"
})
public class ThingsboardUpdateApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "thingsboard";

    public static void main(String[] args) {
        try {
            SpringApplication application = new SpringApplication(ThingsboardUpdateApplication.class);
            application.setAdditionalProfiles("update");
            ConfigurableApplicationContext context = application.run(updateArguments(args));
            context.getBean(ThingsboardUpdateService.class).performInstall();
            log.info("Finished updating!");
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }
}
