package com.koleksiyon.envanter.service;

import com.koleksiyon.envanter.entity.Bid;
import com.koleksiyon.envanter.entity.Item;
import com.koleksiyon.envanter.entity.SystemVault;
import com.koleksiyon.envanter.repository.BidRepository;
import com.koleksiyon.envanter.repository.ItemRepository;
import com.koleksiyon.envanter.repository.SystemVaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.koleksiyon.envanter.entity.User;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor // Lombok ile constructor injection işlemini otomatik yapar
public class ItemService {

    private final ItemRepository itemRepository;
    private final SystemVaultRepository systemVaultRepository;
    private final BidRepository bidRepository;

    // 1. Tüm parçaları listeleme (Read)
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // 2. Yeni parça ekleme (Create) - RESİM İŞLEMLİ
    public void saveItem(Item item, MultipartFile file) throws IOException {
        // Eğer kullanıcı bir dosya yüklediyse ve dosya boş değilse
        if (file != null && !file.isEmpty()) {
            // MultipartFile'ı byte array'e (BLOB) çevirip entity'e set ediyoruz
            item.setImage(file.getBytes());
        }
        itemRepository.save(item);
    }

    // 3. ID'ye göre tek bir parça bulma
    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Geçersiz Parça ID:" + id));
    }

    // 4. Parça silme (Delete)
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    // 5. Dinamik arama işlemi
    public List<Item> searchItems(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return itemRepository.findByNameContainingIgnoreCase(keyword);
        }
        return itemRepository.findAll();
    }
    // SATIŞI TAMAMLAMA VE KOMİSYON KESME METODU
    @Transactional
    public void closeAuction(Long itemId, String loggedInUsername) {
        Item item = getItemById(itemId);

        // Sadece ürünün sahibi satışı kapatabilir
        if (!item.getOwner().getUsername().equals(loggedInUsername)) {
            throw new IllegalStateException("Bu işlemi sadece ürünün sahibi yapabilir!");
        }
        if (item.isSold()) {
            throw new IllegalStateException("Bu ürün zaten satıldı.");
        }
        if (item.getCurrentHighestBid() == null) {
            throw new IllegalStateException("Ürüne henüz teklif verilmemiş.");
        }

        // 1. Ürünü satıldı olarak işaretle
        item.setSold(true);
        itemRepository.save(item);

        // 2. Yüzde 10 Komisyonu Kasaya Ekle
        Double commissionAmount = item.getCurrentHighestBid() * 0.10;

        SystemVault vault = systemVaultRepository.findById(1L).orElse(new SystemVault());
        vault.setId(1L);
        vault.setTotalCommission(vault.getTotalCommission() + commissionAmount);
        systemVaultRepository.save(vault);
    }

    // Teklif Verme Metodu
    @Transactional
    public void placeBid(Long itemId, Double amount, User bidder) {
        Item item = getItemById(itemId);

        if (!item.isForSale() || item.isSold()) {
            throw new IllegalStateException("Bu ürün şu anda tekliflere açık değil.");
        }
        if (item.getOwner().getUsername().equals(bidder.getUsername())) {
            throw new IllegalStateException("Kendi ürününüze teklif veremezsiniz.");
        }
        // Teklifin başlangıç fiyatından ve mevcut en yüksek tekliften büyük olması gerekir
        if (amount == null || amount <= item.getStartingPrice()
                || (item.getCurrentHighestBid() != null && amount <= item.getCurrentHighestBid())) {
            throw new IllegalArgumentException("Teklif miktarı mevcut fiyattan yüksek olmalıdır!");
        }

        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bidRepository.save(bid);

        item.setCurrentHighestBid(amount);
        itemRepository.save(item);
    }

    public List<Bid> getBidsForItem(Long itemId) {
        return bidRepository.findByItemIdOrderByAmountDesc(itemId);
    }

    // Sadece satışta olanları getiren temel metot
    public List<Item> getItemsForSale(String sort, String category) {
        if (category != null && !category.isEmpty()) {
            return itemRepository.findByForSaleTrueAndTypeContainingIgnoreCase(category);
        }

        if (sort != null) {
            switch (sort) {
                case "newest": return itemRepository.findByForSaleTrueOrderByCreatedAtDesc();
                case "price_asc": return itemRepository.findByForSaleTrueOrderByStartingPriceAsc();
                // Varsayılan olarak satıştakileri getir
                default: return itemRepository.findByForSaleTrue();
            }
        }
        return itemRepository.findByForSaleTrue();
    }

    // Kullanıcının koleksiyonundaki (satılık olsun olmasın) her şeyi getirmek için
    public List<Item> getItemsByOwner(User owner) {
        return itemRepository.findByOwner(owner);
    }

}