package dev.sunnyday.arch.mvi.test;

import io.mockk.ConstantMatcher;
import io.mockk.MockKMatcherScope;
import kotlin.reflect.KClass;
import org.jetbrains.annotations.NotNull;

class AnyProvider implements MockProvider {

    private final MockKMatcherScope matcherScope;

    public AnyProvider(MockKMatcherScope matcherScope) {
        this.matcherScope = matcherScope;
    }

    @NotNull
    @Override
    public <T> T getMock(@NotNull String name, @NotNull KClass<T> klass) {
        //noinspection KotlinInternalInJava
        return matcherScope.getCallRecorder()
                .matcher(new ConstantMatcher<T>(true), klass);
    }
}
