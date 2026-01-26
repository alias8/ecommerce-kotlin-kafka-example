package org.example.ecommerceexamplebackendkotlinkafka.product

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductController(private val productService: ProductService) {

    @PutMapping
    fun updateProduct(@Valid @RequestBody updatedProduct: ProductUpdateRequest): ResponseEntity<Product> {
        val updateProduct = productService.updateProduct(updatedProduct)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(updateProduct)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.fieldErrors.map { fieldError ->
            mapOf(
                "field" to fieldError.field,
                "message" to (fieldError.defaultMessage ?: "Invalid value")
            )
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf("errors" to errors)
        )
    }

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            e.message
        )
    }
}