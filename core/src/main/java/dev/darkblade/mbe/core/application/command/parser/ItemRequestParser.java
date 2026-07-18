package dev.darkblade.mbe.core.application.command.parser;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemModifier;
import dev.darkblade.mbe.api.item.ItemRequest;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.util.NamespacedKey;
import dev.darkblade.mbe.core.internal.item.ItemRequestImpl;
import net.kyori.adventure.key.Key;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemRequestParser<C> implements ArgumentParser<C, ItemRequest>, SuggestionProvider<C> {

    private final ItemService itemService;

    public ItemRequestParser(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public ArgumentParseResult<ItemRequest> parse(CommandContext<C> commandContext, CommandInput commandInput) {
        String input = commandInput.readString();
        try {
            return ArgumentParseResult.success(parseString(input));
        } catch (IllegalArgumentException e) {
            return ArgumentParseResult.failure(e);
        }
    }

    public ItemRequest parseString(String input) throws IllegalArgumentException {
        // Remove spaces for easier parsing, if any were typed inside a greedy string
        input = input.replace(" ", "");

        int bracketIndex = input.indexOf('[');
        String idPart = bracketIndex == -1 ? input : input.substring(0, bracketIndex);
        
        NamespacedKey itemId = NamespacedKey.parse(idPart);

        // We assume version 0 for now since ItemKey needs a version. Or we search the registry for the latest version.
        // Actually, we can just look up by ID if we have a way, but ItemKey requires version.
        // The registry should probably be queried. Let's find the definition first.
        ItemDefinition definition = null;
        for (ItemDefinition def : itemService.registry().all()) {
            if (def.key().id().equals(itemId)) {
                definition = def;
                break;
            }
        }
        
        if (definition == null) {
            throw new IllegalArgumentException("Unknown item: " + idPart);
        }

        Map<Key, Object> parsedModifiers = new HashMap<>();

        if (bracketIndex != -1) {
            if (!input.endsWith("]")) {
                throw new IllegalArgumentException("Missing closing bracket ']' in item request");
            }
            String modifiersPart = input.substring(bracketIndex + 1, input.length() - 1);
            if (!modifiersPart.isEmpty()) {
                String[] assignments = modifiersPart.split(",");
                for (String assignment : assignments) {
                    String[] kv = assignment.split("=", 2);
                    if (kv.length != 2) {
                        throw new IllegalArgumentException("Invalid modifier format, expected key=value: " + assignment);
                    }
                    String modKeyStr = kv[0];
                    String modValueStr = kv[1];
                    
                    Key modKey;
                    try {
                        modKey = Key.key(modKeyStr);
                    } catch (Exception e) {
                         throw new IllegalArgumentException("Invalid modifier key format: " + modKeyStr);
                    }

                    Optional<ItemModifier<CommandSender, ?>> optMod = itemService.modifiers().getModifier(itemId, modKey);
                    if (optMod.isEmpty()) {
                        throw new IllegalArgumentException("Unknown modifier '" + modKeyStr + "' for item " + idPart);
                    }
                    
                    try {
                        Object parsed = optMod.get().parse(modValueStr);
                        parsedModifiers.put(modKey, parsed);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for modifier '" + modKeyStr + "': " + e.getMessage());
                    }
                }
            }
        }

        return new ItemRequestImpl(definition.key(), parsedModifiers);
    }

    @Override
    public CompletableFuture<Iterable<Suggestion>> suggestionsFuture(CommandContext<C> context, CommandInput input) {
        String currentInput = input.peekString(); // We use peekString to not consume it during suggestions
        if (currentInput.isEmpty()) {
            // Suggest all item IDs
            return CompletableFuture.completedFuture(
                itemService.registry().all().stream()
                    .map(def -> Suggestion.suggestion(def.key().id().toString()))
                    .collect(Collectors.toList())
            );
        }

        int bracketIndex = currentInput.indexOf('[');
        if (bracketIndex == -1) {
            // Still typing the item ID
            List<Suggestion> suggestions = itemService.registry().all().stream()
                .map(def -> def.key().id().toString())
                .filter(id -> id.startsWith(currentInput.toLowerCase()))
                .map(Suggestion::suggestion)
                .collect(Collectors.toList());
            
            // If the typed text exactly matches an item ID, suggest adding a bracket
            if (suggestions.size() == 1 && suggestions.get(0).suggestion().equals(currentInput)) {
                suggestions.add(Suggestion.suggestion(currentInput + "["));
            }
            return CompletableFuture.completedFuture(suggestions);
        }

        // Inside brackets
        String idPart = currentInput.substring(0, bracketIndex);
        NamespacedKey itemId;
        try {
            itemId = NamespacedKey.parse(idPart);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(List.of());
        }

        String insideBrackets = currentInput.substring(bracketIndex + 1);
        int lastComma = insideBrackets.lastIndexOf(',');
        String currentPart = lastComma == -1 ? insideBrackets : insideBrackets.substring(lastComma + 1);
        String prefix = currentInput.substring(0, bracketIndex + 1 + (lastComma == -1 ? 0 : lastComma + 1));
        
        int equalsIndex = currentPart.indexOf('=');
        
        if (equalsIndex == -1) {
            // Typing modifier key
            String typedKey = currentPart;
            List<Suggestion> suggestions = new ArrayList<>();
            for (ItemModifier<CommandSender, ?> modifier : itemService.modifiers().getModifiers(itemId)) {
                String modStr = modifier.id().asString();
                if (modStr.startsWith(typedKey.toLowerCase())) {
                    suggestions.add(Suggestion.suggestion(prefix + modStr + "="));
                }
            }
            if (suggestions.isEmpty() && currentPart.isEmpty()) {
                suggestions.add(Suggestion.suggestion(prefix + "]")); // Suggest closing bracket if empty
            }
            return CompletableFuture.completedFuture(suggestions);
        } else {
            // Typing modifier value
            String modKeyStr = currentPart.substring(0, equalsIndex);
            String typedValue = currentPart.substring(equalsIndex + 1);
            String valuePrefix = prefix + modKeyStr + "=";
            
            Key modKey;
            try {
                modKey = Key.key(modKeyStr);
            } catch (Exception e) {
                return CompletableFuture.completedFuture(List.of());
            }

            Optional<ItemModifier<CommandSender, ?>> optMod = itemService.modifiers().getModifier(itemId, modKey);
            if (optMod.isPresent()) {
                List<String> validValues = optMod.get().suggestions((CommandContext<CommandSender>) (Object) context);
                List<Suggestion> suggestions = validValues.stream()
                    .filter(v -> v.toLowerCase().startsWith(typedValue.toLowerCase()))
                    .map(v -> Suggestion.suggestion(valuePrefix + v))
                    .collect(Collectors.toList());
                    
                // If it exactly matches a valid value, suggest closing bracket or comma
                if (suggestions.size() == 1 && suggestions.get(0).suggestion().equals(valuePrefix + typedValue)) {
                    List<Suggestion> nextSuggestions = new ArrayList<>();
                    nextSuggestions.add(Suggestion.suggestion(valuePrefix + typedValue + "]"));
                    nextSuggestions.add(Suggestion.suggestion(valuePrefix + typedValue + ","));
                    return CompletableFuture.completedFuture(nextSuggestions);
                }
                
                return CompletableFuture.completedFuture(suggestions);
            }
        }

        return CompletableFuture.completedFuture(List.of());
    }
}
