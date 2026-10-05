package wtf.dupers.dupersunited.api.command.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.module.Module;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ModuleArgumentType implements ArgumentType<Module> {
    private static final ModuleArgumentType INSTANCE = new ModuleArgumentType();
    private static final SimpleCommandExceptionType MODULE_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Module not found."));

    protected ModuleArgumentType() {}

    public static ModuleArgumentType module() {
        return INSTANCE;
    }

    public static <S extends SharedSuggestionProvider> Module get(CommandContext<S> context) {
        return context.getArgument("module", Module.class);
    }

    public static <S extends SharedSuggestionProvider> Module get(CommandContext<S> context, String name) {
        return context.getArgument(name, Module.class);
    }

    @Override
    public Module parse(StringReader stringReader) throws CommandSyntaxException {
        String moduleName = stringReader.readUnquotedString();

        @Nullable Module module = MainClient.getModule(moduleName);
        if (module == null) {
            throw MODULE_NOT_FOUND_EXCEPTION.create();
        }

        return module;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Stream<String> stream = MainClient.addonsPresent
            ? MainClient.getModules().stream().flatMap((m) -> Stream.of(m.getName(), m.getIdentifier()))
            : MainClient.getModules().stream().map(Module::getName);

        return SharedSuggestionProvider.suggest(stream, builder);
    }
}