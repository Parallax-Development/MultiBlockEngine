package dev.darkblade.mbe.core.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import dev.darkblade.mbe.core.domain.action.SendMessageAction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "dev.darkblade.mbe")
public class NoHardcodedMessagesTest {

    @ArchTest
    static final ArchRule no_player_sendmessage_with_literal =
            noClasses()
                    .should().callMethod(Player.class, "sendMessage", String.class)
                    .because("Player messages must use I18nService");

    @ArchTest
    static final ArchRule no_commandsender_sendmessage_with_literal =
            noClasses()
                    .that().resideOutsideOfPackages("..api.i18n..", "..core.infrastructure.i18n..")
                    .should().callMethod(CommandSender.class, "sendMessage", String.class)
                    .because("Sender messages must use I18nService");

    @ArchTest
    static final ArchRule send_message_action_no_string_fields =
            noFields()
                    .that().areDeclaredInClassesThat().areAssignableTo(SendMessageAction.class)
                    .should().haveRawType(String.class)
                    .because("SendMessageAction must be typed with MessageKey + params");
}
