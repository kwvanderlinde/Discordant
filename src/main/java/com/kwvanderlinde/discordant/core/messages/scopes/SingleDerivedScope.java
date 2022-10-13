package com.kwvanderlinde.discordant.core.messages.scopes;

import javax.annotation.Nonnull;

// TODO I want to be able to derive from multiple scopes. E.g., PlayerScope inherits from ProfileScope, DiscordUserScope, and PlayerImageScope.
public interface SingleDerivedScope<Base extends Scope> extends DerivedScope<DerivedScope.Appended<DerivedScope.None, Base>> {
    @Nonnull Base base();

    default @Nonnull Appended<None, Base> bases() {
        return new Appended<>(new DerivedScope.None(), base());
    }
}
