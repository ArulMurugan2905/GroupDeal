package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.GroupDealRepository
import com.example.core.data.LocalityService
import com.example.core.engine.PricingEngine
import com.example.core.model.PricingTier
import com.example.core.model.TrustTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GroupDeal", appName)
  }

  @Test
  fun `pricing engine calculates correct dynamic discount based on active tier`() {
    val tiers = listOf(
      PricingTier(minMembers = 5, unitPrice = 1000.0, discountPercent = 20, label = "Tier 1"),
      PricingTier(minMembers = 15, unitPrice = 800.0, discountPercent = 35, label = "Tier 2"),
      PricingTier(minMembers = 30, unitPrice = 600.0, discountPercent = 50, label = "Tier 3")
    )
    val basePrice = 1200.0

    // 0 members -> base price
    assertEquals(1200.0, PricingEngine.calculateUnitPrice(basePrice, tiers, 0), 0.01)
    // 3 members -> base price (below min tier)
    assertEquals(1200.0, PricingEngine.calculateUnitPrice(basePrice, tiers, 3), 0.01)
    // 5 members -> Tier 1 (1000.0)
    assertEquals(1000.0, PricingEngine.calculateUnitPrice(basePrice, tiers, 5), 0.01)
    // 18 members -> Tier 2 (800.0)
    assertEquals(800.0, PricingEngine.calculateUnitPrice(basePrice, tiers, 18), 0.01)
    // 35 members -> Tier 3 (600.0)
    assertEquals(600.0, PricingEngine.calculateUnitPrice(basePrice, tiers, 35), 0.01)
  }

  @Test
  fun `locality service correctly resolves pincode and searches areas`() {
    val koramangala = LocalityService.lookupPincode("560034")
    assertNotNull(koramangala)
    assertEquals("Koramangala", koramangala?.localityName)
    assertEquals("Bengaluru", koramangala?.city)

    val searchResults = LocalityService.searchLocalities("Mumbai")
    assertTrue(searchResults.isNotEmpty())
  }

  @Test
  fun `trust score tier calculation properly maps scores`() {
    assertEquals(TrustTier.BRONZE, TrustTier.fromScore(250))
    assertEquals(TrustTier.SILVER, TrustTier.fromScore(500))
    assertEquals(TrustTier.GOLD, TrustTier.fromScore(780))
    assertEquals(TrustTier.PLATINUM, TrustTier.fromScore(950))
  }
}
