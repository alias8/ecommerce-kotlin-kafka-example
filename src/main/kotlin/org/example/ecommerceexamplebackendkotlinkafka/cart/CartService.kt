package org.example.ecommerceexamplebackendkotlinkafka.cart

import org.example.ecommerceexamplebackendkotlinkafka.product.ProductRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CartService(
    private val redisTemplate: StringRedisTemplate,
    private val productRepository: ProductRepository
) {
    companion object {
        private val CART_TTL: Duration = Duration.ofDays(7)
    }

    fun getItems(email: String): Map<String, Int> =
        redisTemplate.opsForHash<String, String>().entries(cartKey(email))
            .mapValues { it.value.toInt() }

    fun addItem(email: String, skuId: String, quantity: Int) {
        val key = cartKey(email)
        redisTemplate.opsForHash<String, String>().increment(key, skuId, quantity.toLong())
        redisTemplate.expire(key, CART_TTL)
    }

    fun setItemQuantity(email: String, skuId: String, quantity: Int) {
        val key = cartKey(email)
        redisTemplate.opsForHash<String, String>().put(key, skuId, quantity.toString())
        redisTemplate.expire(key, CART_TTL)
    }

    fun removeItem(email: String, skuId: String) {
        redisTemplate.opsForHash<String, String>().delete(cartKey(email), skuId)
    }

    fun clear(email: String) {
        redisTemplate.delete(cartKey(email))
    }

    fun viewCart(email: String): CartResponse {
        val items = getItems(email).mapNotNull { (skuId, quantity) ->
            val product = productRepository.findBySkuId(skuId) ?: return@mapNotNull null
            CartItemView(skuId, quantity, product.unitPrice, product.unitPrice * quantity)
        }
        return CartResponse(items, items.sumOf { it.lineTotal })
    }

    private fun cartKey(email: String) = "cart:$email"
}
