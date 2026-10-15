package com.koleksiyon.envanter.controller;

import com.koleksiyon.envanter.dto.ItemDTO;
import com.koleksiyon.envanter.entity.Item;
import com.koleksiyon.envanter.entity.User;
import com.koleksiyon.envanter.repository.ItemRepository;
import com.koleksiyon.envanter.service.ItemService;
import com.koleksiyon.envanter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;
    private final ItemRepository itemRepository; // Hataları önlemek için doğrudan repository eklendi

    // Giriş yapmış kullanıcıyı getiren yardımcı metot
    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName());
    }

    // 1. ANA SAYFA: Sadece satışta olan ürünleri filtreli gösterir
    @GetMapping
    public String listItems(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        List<Item> items;

        if (search != null && !search.isEmpty()) {
            items = itemRepository.findByNameContainingIgnoreCase(search);
        } else {
            // Servis üzerinden filtreli satış listesini getirir
            items = itemService.getItemsForSale(sort, category);
        }

        model.addAttribute("items", items);
        model.addAttribute("currentUser", getLoggedInUser());
        return "index";
    }

    // 2. ÜRÜN EKLEME FORMU
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("itemDTO", new ItemDTO());
        return "item-form";
    }

    // 3. KOLEKSİYONA KAYDETME (Satışta Değil Olarak Başlar)
    @PostMapping("/save")
    public String saveItem(@ModelAttribute("itemDTO") ItemDTO itemDTO) throws IOException {
        Item item = new Item();
        item.setName(itemDTO.getName());
        item.setType(itemDTO.getType());
        item.setDescription(itemDTO.getDescription());
        item.setStartingPrice(itemDTO.getStartingPrice());

        item.setOwner(getLoggedInUser());
        item.setForSale(false); // Önce sadece koleksiyona eklenir

        itemService.saveItem(item, itemDTO.getImageFile());
        return "redirect:/my-collection";
    }

    // 4. ÜRÜNÜ SATIŞA ÇIKARMA
    @PostMapping("/list-for-sale/{id}")
    @Transactional // Veritabanı oturumunun açık kalmasını sağlar
    public String listForSale(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        User currentUser = getLoggedInUser();

        if (item.getOwner().getUsername().equals(currentUser.getUsername())) {
            item.setForSale(true);
            itemRepository.save(item);
        }
        return "redirect:/items";
    }

    // 5. ÜRÜN DETAY SAYFASI
    @GetMapping("/{id}")
    public String showItemDetails(@PathVariable Long id, Model model) {
        Item item = itemService.getItemById(id);
        model.addAttribute("item", item);
        model.addAttribute("bids", itemService.getBidsForItem(id));
        model.addAttribute("currentUser", getLoggedInUser());
        return "item-details";
    }

    // 6. TEKLİF VERME
    @PostMapping("/bid/{id}")
    public String placeBid(@PathVariable Long id, @RequestParam("amount") Double amount,
                           RedirectAttributes redirect) {
        try {
            itemService.placeBid(id, amount, getLoggedInUser());
            redirect.addFlashAttribute("success", "Teklifiniz alındı.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items/" + id;
    }

    // 6b. SATIŞI KAPATMA (sadece ürün sahibi, %10 komisyon kesilir)
    @PostMapping("/close/{id}")
    public String closeAuction(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            itemService.closeAuction(id, getLoggedInUser().getUsername());
            redirect.addFlashAttribute("success", "Satış tamamlandı.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items/" + id;
    }

    // 7. SİLME (Admin veya sahibi silebilir) - durum değiştiren işlem olduğu için POST
    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        User currentUser = getLoggedInUser();

        if ("ROLE_ADMIN".equals(currentUser.getRole()) ||
                item.getOwner().getUsername().equals(currentUser.getUsername())) {
            itemService.deleteItem(id);
        }
        return "redirect:/items";
    }

    // 8. RESİM GETİRME
    @GetMapping("/image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getItemImage(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        if (item.getImage() != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(item.getImage());
        }
        return ResponseEntity.notFound().build();
    }


}