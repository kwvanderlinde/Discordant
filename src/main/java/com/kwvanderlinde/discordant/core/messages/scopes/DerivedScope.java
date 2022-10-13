package com.kwvanderlinde.discordant.core.messages.scopes;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.discordant.core.messages.SemanticMessage;

import javax.annotation.Nonnull;
import java.util.Map;

// TODO I want to be able to derive from multiple scopes. E.g., PlayerScope inherits from ProfileScope, DiscordUserScope, and PlayerImageScope.
public interface DerivedScope<D extends DerivedScope.Derivation> extends Scope {
    @Nonnull D bases();

    @Nonnull Map<String, SemanticMessage.Part> notInheritedValues();

    @Override
    default @Nonnull Map<String, SemanticMessage.Part> values() {
        final var resultBuilder = ImmutableMap.<String, SemanticMessage.Part> builder();
        resultBuilder.putAll(notInheritedValues());
        bases().addValues(resultBuilder);

        return resultBuilder.build();
    }

    default None derivation() {
        return new None();
    }

    sealed interface Derivation permits None, Appended {
        void addValues(ImmutableMap.Builder<String, SemanticMessage.Part> builder);
    }
    record None() implements Derivation {
        @Override
        public void addValues(ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
        }

        <T extends Scope> Appended<None, T> with(T scope) {
            return new Appended<>(new None(), scope);
        }
    }
    record Appended<Preceding extends Derivation, Base extends Scope>(Preceding preceding, Base base) implements Derivation {
        @Override
        public void addValues(ImmutableMap.Builder<String, SemanticMessage.Part> builder) {
            builder.putAll(base.values());
            preceding.addValues(builder);
        }

        public <T extends Scope> Appended<Appended<Preceding, Base>, T> with(T scope) {
            return new Appended<>(this, scope);
        }
    }
}
