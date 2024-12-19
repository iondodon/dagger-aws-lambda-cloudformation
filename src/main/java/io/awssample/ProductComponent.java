package io.awssample;

import dagger.Component;
import io.awssample.handler.SQSProductHandler;

import javax.inject.Singleton;

@Singleton
@Component(modules = ProductModule.class)
public interface ProductComponent {

    void inject(SQSProductHandler sqsProductHandler);

}
