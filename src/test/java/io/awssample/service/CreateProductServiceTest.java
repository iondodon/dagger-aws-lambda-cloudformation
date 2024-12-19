package io.awssample.service;

import io.awssample.model.Product;
import io.awssample.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    CreateProductService createProductService;

    @ParameterizedTest
    @ValueSource(strings = {"   ", ""})
    @NullSource
    @DisplayName("Should fail when product name is blank or null")
    void shouldFailWhenProductNameIsBlank(String productName) {
        // when: Attempting to create a product with a blank or null name
        IllegalArgumentException thrown = catchThrowableOfType(
                IllegalArgumentException.class,
                () -> createProductService.create(productName, ONE)
        );

        // then: An exception is thrown with the appropriate message
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name cannot be blank");
    }

    @Test
    @DisplayName("Should fail when product name exceeds 255 characters")
    void shouldFailWhenProductNameOverflows() {
        // given: a 256-character product name
        var productName = RandomStringUtils.random(256, true, true);

        // when: Attempting to create a product with a null price
        IllegalArgumentException thrown = catchThrowableOfType(
                IllegalArgumentException.class,
                () -> createProductService.create(productName, ONE)
        );

        // then: An exception is thrown with the appropriate message
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name cannot exceed 255 characters");
    }

    @Test
    @DisplayName("Should fail when product price is null")
    void shouldFailWhenProductPriceIsNull() {
        // when: Attempting to create a product with a null price
        NullPointerException thrown = catchThrowableOfType(
                NullPointerException.class,
                () -> createProductService.create("Banana", null)
        );

        // then: An exception is thrown with the appropriate message
        assertThat(thrown)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Price cannot be null");
    }

    @Test
    @DisplayName("Should fail when product price is not greater than zero")
    void shouldFailWhenProductPriceIsNotGreaterThanZero() {
        // when: Attempting to create a product with a non-positive price
        IllegalArgumentException thrown = catchThrowableOfType(
                IllegalArgumentException.class,
                () -> createProductService.create("Banana", ZERO)
        );

        // then: An exception is thrown with the appropriate message
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price must be greater than zero");
    }

    @Test
    @DisplayName("Should fail when saving the product to the repository fails")
        // FIXME: this test is useless, just a simulation of DB exception
    void shouldFailWhenProductPutFails() {
        // given: The repository throws an exception when attempting to save the product
        doThrow(RuntimeException.class).when(productRepository).put(any(Product.class));

        // when: Attempting to create a product
        RuntimeException thrown = catchThrowableOfType(
                RuntimeException.class,
                () -> createProductService.create("Banana", ONE)
        );

        // then: An exception is thrown
        assertThat(thrown)
                .isInstanceOf(RuntimeException.class);

        // and: The repository's save method is invoked
        verify(productRepository).put(any(Product.class));
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProductSuccessfully() {
        // when: Creating a product with valid name and price
        createProductService.create("Banana", ONE);

        // then: The product is saved to the repository with the correct details
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).put(productCaptor.capture());

        // not null id and it's an instance of UUID.call
        assertThat(productCaptor.getValue())
                .isNotNull()
                .extracting(product -> UUID.fromString(product.id()))
                .isNotNull();

        // assert product details
        assertThat(productCaptor.getValue())
                .extracting(Product::name, Product::price)
                .containsExactly("Banana", ONE);
    }
}
