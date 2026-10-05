package wtf.dupers.dupersunited.mixin.network;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import wtf.dupers.dupersunited.utils.IClientCommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@Mixin(ClientSuggestionProvider.class)
public class ClientCommandSourceMixin implements IClientCommandSource {
    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestionsFuture;
    @Shadow private int pendingSuggestionsId;

    @Override
    public int dupersunited$beginCompletion() {
        if (this.pendingSuggestionsFuture != null) {
            this.pendingSuggestionsFuture.cancel(false);
        }

        this.pendingSuggestionsFuture = new CompletableFuture<>();
        return ++this.pendingSuggestionsId;
    }

    @Override
    public void dupersunited$endCompletion() {
        this.pendingSuggestionsFuture = null;
        this.pendingSuggestionsId = -1;
    }
}