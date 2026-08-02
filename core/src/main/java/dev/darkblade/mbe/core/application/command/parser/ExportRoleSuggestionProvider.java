package dev.darkblade.mbe.core.application.command.parser;

import dev.darkblade.mbe.core.internal.tooling.export.ExportRoles;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Provides tab-completion suggestions for the {@code mark <role>} argument of the export command.
 *
 * <p>Suggests the well-known roles ({@code controller}, {@code input}, {@code output}) defined in
 * {@link ExportRoles}. Custom/addon roles are free-form and therefore cannot be enumerated here,
 * but the known ones cover the most common workflow.</p>
 *
 * @param <C> command sender type
 */
public final class ExportRoleSuggestionProvider<C> implements SuggestionProvider<C> {

    private static final List<Suggestion> SUGGESTIONS = ExportRoles.KNOWN.stream()
            .map(Suggestion::suggestion)
            .collect(Collectors.toUnmodifiableList());

    @Override
    public @NonNull CompletableFuture<Iterable<@NonNull Suggestion>> suggestionsFuture(
            @NonNull CommandContext<C> context, @NonNull CommandInput input) {
        return CompletableFuture.completedFuture(SUGGESTIONS);
    }
}
