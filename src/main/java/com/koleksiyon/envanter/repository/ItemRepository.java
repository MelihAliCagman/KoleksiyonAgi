package com.koleksiyon.envanter.repository;

import com.koleksiyon.envanter.entity.Item;
import com.koleksiyon.envanter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByNameContainingIgnoreCase(String name);

    // Sadece satışta olan tüm ürünleri getir
    List<Item> findByForSaleTrue();

    // Satışta olanları tarihe göre (en yeni) sırala
    List<Item> findByForSaleTrueOrderByCreatedAtDesc();

    // Satışta olanları fiyata göre artan sırala
    List<Item> findByForSaleTrueOrderByStartingPriceAsc();

    // Kategoriye (Tür) göre arama yap ve satışta olanları getir
    List<Item> findByForSaleTrueAndTypeContainingIgnoreCase(String type);

    // Bir kullanıcının koleksiyonundaki tüm ürünler
    List<Item> findByOwner(User owner);
}
