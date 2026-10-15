package com.koleksiyon.envanter;

import com.koleksiyon.envanter.entity.Item;
import com.koleksiyon.envanter.entity.SystemVault;
import com.koleksiyon.envanter.entity.User;
import com.koleksiyon.envanter.repository.BidRepository;
import com.koleksiyon.envanter.repository.ItemRepository;
import com.koleksiyon.envanter.repository.SystemVaultRepository;
import com.koleksiyon.envanter.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private ItemRepository itemRepository;
    private SystemVaultRepository vaultRepository;
    private BidRepository bidRepository;
    private ItemService service;
    private User owner;
    private User bidder;
    private Item item;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        vaultRepository = mock(SystemVaultRepository.class);
        bidRepository = mock(BidRepository.class);
        service = new ItemService(itemRepository, vaultRepository, bidRepository);

        owner = new User();
        owner.setUsername("sahip");
        bidder = new User();
        bidder.setUsername("alici");

        item = new Item();
        item.setId(1L);
        item.setOwner(owner);
        item.setForSale(true);
        item.setStartingPrice(100.0);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
    }

    @Test
    void validBidIsRecordedAndRaisesHighestBid() {
        service.placeBid(1L, 150.0, bidder);

        assertEquals(150.0, item.getCurrentHighestBid());
        verify(bidRepository).save(any());
    }

    @Test
    void bidNotAboveCurrentPriceIsRejected() {
        item.setCurrentHighestBid(200.0);

        assertThrows(IllegalArgumentException.class, () -> service.placeBid(1L, 200.0, bidder));
        verify(bidRepository, never()).save(any());
    }

    @Test
    void ownerCannotBidOnOwnItem() {
        assertThrows(IllegalStateException.class, () -> service.placeBid(1L, 150.0, owner));
    }

    @Test
    void bidOnItemNotForSaleIsRejected() {
        item.setForSale(false);

        assertThrows(IllegalStateException.class, () -> service.placeBid(1L, 150.0, bidder));
    }

    @Test
    void closingAuctionAddsTenPercentCommission() {
        item.setCurrentHighestBid(500.0);
        SystemVault vault = new SystemVault();
        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));

        service.closeAuction(1L, "sahip");

        assertTrue(item.isSold());
        assertEquals(50.0, vault.getTotalCommission(), 0.001);
    }

    @Test
    void onlyOwnerCanCloseAuction() {
        item.setCurrentHighestBid(500.0);

        assertThrows(IllegalStateException.class, () -> service.closeAuction(1L, "alici"));
    }
}
