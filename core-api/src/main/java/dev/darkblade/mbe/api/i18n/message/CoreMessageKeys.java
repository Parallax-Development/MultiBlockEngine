package dev.darkblade.mbe.api.i18n.message;

import dev.darkblade.mbe.api.i18n.MessageKey;

public enum CoreMessageKeys implements MessageKey {
    COMMAND_NO_PERMISSION("commands.error.no_permission"),
    COMMAND_PLAYER_ONLY("commands.error.player_only"),
    COMMAND_ADMIN_USAGE("commands.admin.usage"),
    COMMAND_DEBUG_USAGE_LAYER("commands.debug.usage_layer"),
    COMMAND_HELP_INSPECT("commands.help.inspect"),
    COMMAND_HELP_ASSEMBLE("commands.help.assemble"),
    COMMAND_HELP_DISASSEMBLE("commands.help.disassemble"),
    COMMAND_HELP_STATUS("commands.help.status"),
    COMMAND_HELP_REPORT("commands.help.report"),
    COMMAND_HELP_BLUEPRINT_CATALOG("commands.help.blueprint_catalog"),
    COMMAND_HELP_BLUEPRINT_LIST("commands.help.blueprint_list"),
    COMMAND_HELP_BLUEPRINT_GIVE("commands.help.blueprint_give"),
    COMMAND_HELP_ADMIN_RELOAD("commands.help.admin_reload"),
    COMMAND_HELP_ADMIN_EXPORT("commands.help.admin_export"),
    COMMAND_HELP_ADMIN_SERVICES("commands.help.admin_services"),
    COMMAND_HELP_ADMIN_SERVICE("commands.help.admin_service"),
    COMMAND_HELP_DEBUG_TYPE("commands.help.debug_type"),
    COMMAND_HELP_DEBUG_SERVICE("commands.help.debug_service"),
    COMMAND_HELP_DEBUG_LIST("commands.help.debug_list"),
    EXPORT_USAGE("commands.export.usage"),
    EXPORT_STARTED("commands.export.started"),
    EXPORT_CANCELLED("commands.export.cancelled"),
    EXPORT_NO_ACTIVE("commands.export.no_active"),
    EXPORT_MUST_LOOK("commands.export.must_look"),
    EXPORT_POS1_SET("commands.export.pos1_set"),
    EXPORT_POS2_SET("commands.export.pos2_set"),
    EXPORT_MARK_USAGE("commands.export.mark_usage"),
    EXPORT_MARK_PROMPT("commands.export.mark_prompt"),
    EXPORT_SAVE_USAGE("commands.export.save_usage"),
    EXPORT_SAVE_OK("commands.export.save_ok"),
    EXPORT_SAVE_WARNINGS("commands.export.save_warnings"),
    EXPORT_SAVE_ERROR("commands.export.save_error"),
    EXPORT_SUBCOMMAND_INVALID("commands.export.subcommand_invalid"),
    BLUEPRINT_USAGE("commands.blueprint.usage"),
    BLUEPRINT_PLAYER_ONLY_CATALOG("commands.blueprint.player_only_catalog"),
    BLUEPRINT_SERVICES_UNAVAILABLE("commands.blueprint.services_unavailable"),
    BLUEPRINT_NONE_LOADED("commands.blueprint.none_loaded"),
    BLUEPRINT_AVAILABLE_TITLE("commands.blueprint.available_title"),
    BLUEPRINT_ID_REQUIRED("commands.blueprint.id_required"),
    BLUEPRINT_PLAYER_NOT_FOUND("commands.blueprint.player_not_found"),
    BLUEPRINT_CONSOLE_NEEDS_PLAYER("commands.blueprint.console_needs_player"),
    BLUEPRINT_NOT_FOUND("commands.blueprint.not_found"),
    BLUEPRINT_CREATE_FAILED("commands.blueprint.create_failed"),
    BLUEPRINT_RECEIVED("commands.blueprint.received"),
    BLUEPRINT_GIVEN("commands.blueprint.given"),
    DEBUG_SOURCE("commands.debug.source"),
    DEBUG_SIGNATURE("commands.debug.signature"),
    ASSEMBLY_COORDINATOR_UNAVAILABLE("commands.assemble.coordinator_unavailable"),
    ASSEMBLE_MUST_LOOK("commands.assemble.must_look"),
    ASSEMBLE_TRY_FAILED("commands.assemble.try_failed"),
    ASSEMBLE_OK("commands.assemble.ok"),
    ASSEMBLE_FAILED("commands.assemble.failed"),
    DISASSEMBLE_MUST_LOOK("commands.disassemble.must_look"),
    DISASSEMBLE_NONE_HERE("commands.disassemble.none_here"),
    ACTION_CANCELLED("commands.disassemble.action_cancelled"),
    DISASSEMBLED("commands.disassemble.done"),
    WRENCH_INSPECT_COMPONENT_LINE("core.wrench.inspect.component_line"),
    PREVIEW_STARTED("core.preview.started"),
    PREVIEW_CANCELLED("core.preview.cancelled"),
    PREVIEW_COMPLETED("core.preview.completed"),
    DEBUG_SESSION_STARTED("core.debug.session_started"),
    LINK_INVALID_BLOCK("core.ui.link.invalid_block"),
    LINK_ALREADY_EXISTS("core.ui.link.already_exists"),
    LINK_CREATED("core.ui.link.created"),
    LINK_FINISHED("core.ui.link.finished"),
    LINK_CANCELLED("core.ui.link.cancelled"),
    EXPORT_MARKED("core.export.marked");

    private static final String ORIGIN = "mbe";
    private final String path;

    CoreMessageKeys(String path) {
        this.path = path;
    }

    @Override
    public String origin() {
        return ORIGIN;
    }

    @Override
    public String path() {
        return path;
    }
}
