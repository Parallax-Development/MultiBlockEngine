package com.darkbladedev.engine.command.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ServiceCallParser {

    public enum CallMode {
        INFO,
        EXECUTE;

        static Optional<CallMode> parse(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "info" -> Optional.of(INFO);
                case "execute" -> Optional.of(EXECUTE);
                default -> Optional.empty();
            };
        }
    }

    public sealed interface ParseResult permits ParseResult.Ok, ParseResult.Error {
        record Ok(ServiceCall call) implements ParseResult {
        }

        record Error(String message, List<String> hints) implements ParseResult {
            public Error {
                message = message == null ? "" : message;
                hints = hints == null || hints.isEmpty() ? List.of() : List.copyOf(hints);
            }
        }
    }

    public record ServiceCall(String serviceId, CallMode mode, List<String> args) {
        public ServiceCall {
            Objects.requireNonNull(serviceId, "serviceId");
            Objects.requireNonNull(mode, "mode");
            args = args == null ? List.of() : List.copyOf(args);
        }
    }

    public ParseResult parseServicesCall(String[] args) {
        if (args == null || args.length == 0) {
            return new ParseResult.Error("Falta subcomando.", List.of("Uso: /mbe services call <servicio> <info|execute> <argumentos>"));
        }

        if (!"services".equalsIgnoreCase(args[0])) {
            return new ParseResult.Error("Subcomando inválido.", List.of("Uso: /mbe services call <servicio> <info|execute> <argumentos>"));
        }

        if (args.length < 2) {
            return new ParseResult.Error(
                    "Falta acción de services.",
                    List.of("Uso: /mbe services call <servicio> <info|execute> <argumentos>")
            );
        }

        String op = args[1];
        if (!"call".equalsIgnoreCase(op)) {
            return new ParseResult.Error(
                    "Acción de services inválida: " + op,
                    List.of("Usa: /mbe services call <servicio> <info|execute> <argumentos>", "Opcional: /mbe services list")
            );
        }

        if (args.length < 4) {
            return new ParseResult.Error(
                    "Sintaxis incompleta.",
                    List.of("Uso: /mbe services call <servicio> <info|execute> <argumentos>")
            );
        }

        String serviceId = args[2];
        if (serviceId == null || serviceId.isBlank()) {
            return new ParseResult.Error("Servicio vacío.", List.of("Ejemplo: /mbe services call ui info"));
        }

        Optional<CallMode> modeOpt = CallMode.parse(args[3]);
        if (modeOpt.isEmpty()) {
            return new ParseResult.Error(
                    "Modo inválido: " + args[3],
                    List.of("Valores válidos: info | execute", "Ejemplo: /mbe services call ui execute open storage:terminal")
            );
        }

        List<String> callArgs = args.length <= 4
                ? List.of()
                : new ArrayList<>(Arrays.asList(args).subList(4, args.length));

        return new ParseResult.Ok(new ServiceCall(serviceId, modeOpt.get(), callArgs));
    }
}

