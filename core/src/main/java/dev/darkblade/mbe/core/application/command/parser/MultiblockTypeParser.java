package dev.darkblade.mbe.core.application.command.parser;

import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MultiblockTypeParser<C> implements ArgumentParser<C, MultiblockType>, SuggestionProvider<C> {

    private final MultiblockRuntimeService runtimeService;

    public MultiblockTypeParser(MultiblockRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public @NonNull ArgumentParseResult<MultiblockType> parse(@NonNull CommandContext<C> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.readString();
        java.util.Optional<MultiblockType> typeOpt = runtimeService.getType(input);
        if (typeOpt.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Multiblock type '" + input + "' not found."));
        }
        return ArgumentParseResult.success(typeOpt.get());
    }

    @Override
    public @NonNull CompletableFuture<Iterable<@NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<C> context, @NonNull CommandInput input) {
        return CompletableFuture.completedFuture(
                runtimeService.getTypes().stream()
                        .map(type -> Suggestion.suggestion(type.id().toString()))
                        .collect(Collectors.toList())
        );
    }
}
