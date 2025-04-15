package com.example.biofitbe.service;

import com.example.biofitbe.dto.FoodDTO;
import com.example.biofitbe.model.Food;
import com.example.biofitbe.model.User;
import com.example.biofitbe.repository.FoodDoneRepository;
import com.example.biofitbe.repository.FoodRepository;
import com.example.biofitbe.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodService {
    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodDoneRepository foodDoneRepository;


    // Lấy danh sách Food theo userId
    public List<FoodDTO> getFoodsByUserId(Long userId) {
        List<Food> foods = foodRepository.findByUserUserIdOrderByFoodNameAsc(userId);
        return foods.stream().map(FoodDTO::new).collect(Collectors.toList());
    }

    // Lấy thông tin chi tiết của Food
    public FoodDTO getFoodByIdWithDetails(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new RuntimeException("Food not found"));
        return new FoodDTO(food); // ✅ Trả về FoodDTO mà không cần chi tiết riêng nữa
    }

    @Transactional
    public Optional<FoodDTO> createFood(FoodDTO foodDTO) {
        // Kiểm tra xem thực phẩm đã tồn tại chưa
        if (foodRepository.findByUserUserIdAndFoodName(foodDTO.getUserId(), foodDTO.getFoodName()).isPresent()) {
            return Optional.empty();
        }

        // Tìm kiếm User
        Optional<User> userOpt = userRepository.findById(foodDTO.getUserId());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        // Tạo mới Food
        Food food = Food.builder()
                .user(user)
                .foodName(foodDTO.getFoodName())
                .session(foodDTO.getSession())
                .date(foodDTO.getDate())
                .foodImage(foodDTO.getFoodImage()) // Sử dụng trực tiếp byte[], có thể là null
                .servingSize(foodDTO.getServingSize())
                .servingSizeUnit(foodDTO.getServingSizeUnit())
                .mass(foodDTO.getMass())
                .calories(foodDTO.getCalories())
                .protein(foodDTO.getProtein())
                .carbohydrate(foodDTO.getCarbohydrate())
                .fat(foodDTO.getFat())
                .sodium(foodDTO.getSodium())
                .build();

        // Lưu Food vào cơ sở dữ liệu
        food = foodRepository.save(food);

        // Trả về FoodDTO
        FoodDTO createdFoodDTO = new FoodDTO(food);
        return Optional.of(createdFoodDTO);
    }

    @Transactional
    public void deleteFood(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found with ID: " + foodId));

        foodDoneRepository.deleteByFood(food);
        foodRepository.delete(food);
    }

    @Transactional
    public Optional<FoodDTO> updateFood(FoodDTO foodDTO) {
        Optional<Food> foodOpt = foodRepository.findById(foodDTO.getFoodId());
        if (foodOpt.isEmpty()) {
            return Optional.empty();
        }
        Food food = foodOpt.get();

        // Cập nhật các trường
        food.setFoodName(foodDTO.getFoodName());
        food.setSession(foodDTO.getSession());
        food.setDate(foodDTO.getDate());
        if (foodDTO.getFoodImage() != null) {
            food.setFoodImage(foodDTO.getFoodImage());
        }
        food.setServingSize(foodDTO.getServingSize());
        food.setServingSizeUnit(foodDTO.getServingSizeUnit());
        food.setMass(foodDTO.getMass());
        food.setCalories(foodDTO.getCalories());
        food.setProtein(foodDTO.getProtein());
        food.setCarbohydrate(foodDTO.getCarbohydrate());
        food.setFat(foodDTO.getFat());
        food.setSodium(foodDTO.getSodium());

        foodRepository.save(food);
        return Optional.of(new FoodDTO(food));
    }

    @Transactional
    public void initializeDefaultFoods(Long userId) {
        if (foodRepository.countByUserUserId(userId) > 0) {
            return;
        }
        List<FoodDTO> defaultFoods = List.of(
                // Thức ăn gợi ý buổi sáng
                new FoodDTO(null, userId, "Bánh mì ốp la", "2025-01-01", "Morning", loadImage("DefaultFoods/banhmiopla.jpg"), 1f, "serving", 250f, 350f, 12f, 30f, 18f, 550f),
                new FoodDTO(null, userId, "Phở bò", "2025-01-01", "Morning", loadImage("DefaultFoods/phobo.jpg"), 1f, "bowl", 400f, 350f, 20f, 40f, 10f, 800f),
                new FoodDTO(null, userId, "Bún riêu", "2025-01-01", "Morning", loadImage("DefaultFoods/bunrieu.jpg"), 1f, "bowl", 350f, 300f, 15f, 35f, 12f, 750f),
                new FoodDTO(null, userId, "Xôi mặn", "2025-01-01", "Morning", loadImage("DefaultFoods/xoiman.jpg"), 1f, "serving", 300f, 450f, 10f, 45f, 20f, 600f),
                new FoodDTO(null, userId, "Cháo trắng trứng muối", "2025-01-01", "Morning", loadImage("DefaultFoods/chao.jpg"), 1f, "bowl", 300f, 200f, 6f, 25f, 8f, 400f),
                new FoodDTO(null, userId, "Hủ tiếu", "2025-01-01", "Morning", loadImage("DefaultFoods/hutieu.jpg"), 1f, "bowl", 400f, 380f, 15f, 42f, 12f, 700f),
                new FoodDTO(null, userId, "Bánh cuốn", "2025-01-01", "Morning", loadImage("DefaultFoods/banhcuon.jpg"), 1f, "plate", 250f, 320f, 10f, 38f, 10f, 550f),
                new FoodDTO(null, userId, "Bánh bao", "2025-01-01", "Morning", loadImage("DefaultFoods/banhbao.jpg"), 1f, "piece", 200f, 280f, 9f, 35f, 9f, 500f),
                new FoodDTO(null, userId, "Mì gói trứng xúc xích", "2025-01-01", "Morning", loadImage("DefaultFoods/migoixucxich.jpg"), 1f, "bowl", 350f, 420f, 12f, 45f, 20f, 750f),
                new FoodDTO(null, userId, "Cơm tấm sườn bì chả", "2025-01-01", "Morning", loadImage("DefaultFoods/comtam.jpg"), 1f, "plate", 450f, 600f, 25f, 50f, 30f, 950f),
                new FoodDTO(null, userId, "Smoothie bowl", "2025-01-01", "Morning", loadImage("DefaultFoods/smoothiebowl.jpg"), 1f, "bowl", 300f, 250f, 5f, 30f, 8f, 100f),
                new FoodDTO(null, userId, "Trứng luộc + bánh mì nướng", "2025-01-01", "Morning", loadImage("DefaultFoods/trungbanhmi.jpg"), 1f, "serving", 250f, 320f, 14f, 28f, 14f, 420f),
                new FoodDTO(null, userId, "Bánh mì pate", "2025-01-01", "Morning", loadImage("DefaultFoods/banhmipate.jpg"), 1f, "piece", 220f, 340f, 11f, 35f, 15f, 500f),
                new FoodDTO(null, userId, "Trứng ốp + salad", "2025-01-01", "Morning", loadImage("DefaultFoods/trungsalad.jpg"), 1f, "plate", 250f, 270f, 12f, 18f, 16f, 350f),
                new FoodDTO(null, userId, "Ngũ cốc với sữa", "2025-01-01", "Morning", loadImage("DefaultFoods/ngucoc.jpg"), 1f, "bowl", 200f, 200f, 5f, 35f, 4f, 300f),
                new FoodDTO(null, userId, "Yến mạch ngâm", "2025-01-01", "Morning", loadImage("DefaultFoods/yenmach.jpg"), 1f, "bowl", 220f, 240f, 6f, 32f, 7f, 250f),
                new FoodDTO(null, userId, "Sandwich gà", "2025-01-01", "Morning", loadImage("DefaultFoods/sandwichga.jpg"), 1f, "piece", 250f, 320f, 14f, 30f, 12f, 450f),
                new FoodDTO(null, userId, "Sữa đậu + bánh giò", "2025-01-01", "Morning", loadImage("DefaultFoods/banhgio.jpg"), 1f, "serving", 300f, 300f, 9f, 30f, 10f, 500f),
                new FoodDTO(null, userId, "Pancake mật ong", "2025-01-01", "Morning", loadImage("DefaultFoods/pancake.jpg"), 1f, "stack", 280f, 290f, 6f, 35f, 10f, 400f),
                new FoodDTO(null, userId, "bánh mì phô mai", "2025-01-01", "Morning", loadImage("DefaultFoods/matcha.jpg"), 1f, "serving", 350f, 370f, 10f, 38f, 14f, 420f),
                // thức ăn gợi ý buổi trưa
                new FoodDTO(null, userId, "Cơm gà xối mỡ", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comgaxoimo.jpg"), 1f, "plate", 450f, 600f, 30f, 50f, 25f, 800f),
                new FoodDTO(null, userId, "Bún thịt nướng", "2025-01-01", "Afternoon", loadImage("DefaultFoods/bunthitnuong.jpg"), 1f, "bowl", 400f, 500f, 25f, 45f, 20f, 700f),
                new FoodDTO(null, userId, "Cơm sườn ram mặn", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comsuonram.jpg"), 1f, "plate", 450f, 580f, 28f, 48f, 22f, 850f),
                new FoodDTO(null, userId, "Bún bò Huế", "2025-01-01", "Afternoon", loadImage("DefaultFoods/bunbohue.jpg"), 1f, "bowl", 500f, 480f, 30f, 50f, 12f, 1100f),
                new FoodDTO(null, userId, "Cơm chiên Dương Châu", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comchienduongchau.jpg"), 1f, "plate", 400f, 550f, 15f, 55f, 20f, 600f),
                new FoodDTO(null, userId, "Cơm tấm", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comtam.jpg"), 1f, "plate", 450f, 620f, 27f, 50f, 28f, 950f),
                new FoodDTO(null, userId, "Mì Quảng", "2025-01-01", "Afternoon", loadImage("DefaultFoods/miquang.jpg"), 1f, "bowl", 400f, 460f, 18f, 38f, 16f, 800f),
                new FoodDTO(null, userId, "Lẩu cá", "2025-01-01", "Afternoon", loadImage("DefaultFoods/lauca.jpg"), 1f, "serving", 500f, 400f, 35f, 25f, 18f, 900f),
                new FoodDTO(null, userId, "Canh chua + cá kho", "2025-01-01", "Afternoon", loadImage("DefaultFoods/canhchua.jpg"), 1f, "serving", 450f, 520f, 26f, 35f, 20f, 850f),
                new FoodDTO(null, userId, "Cơm gà nướng mật ong", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comganuong.jpg"), 1f, "plate", 450f, 580f, 30f, 45f, 22f, 780f),
                new FoodDTO(null, userId, "Gỏi cuốn", "2025-01-01", "Afternoon", loadImage("DefaultFoods/goicuon.jpg"), 3f, "pieces", 300f, 250f, 10f, 30f, 6f, 450f),
                new FoodDTO(null, userId, "Bánh canh cua", "2025-01-01", "Afternoon", loadImage("DefaultFoods/banhcanh.jpg"), 1f, "bowl", 400f, 420f, 20f, 35f, 10f, 700f),
                new FoodDTO(null, userId, "Cơm trứng + đậu hũ sốt cà", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comtrungdauhu.jpg"), 1f, "plate", 400f, 480f, 18f, 50f, 14f, 650f),
                new FoodDTO(null, userId, "Cơm thịt kho trứng", "2025-01-01", "Afternoon", loadImage("DefaultFoods/thitkhotrung.jpg"), 1f, "plate", 450f, 600f, 25f, 45f, 22f, 800f),
                new FoodDTO(null, userId, "Cá hấp xì dầu", "2025-01-01", "Afternoon", loadImage("DefaultFoods/cahapxidau.jpg"), 1f, "plate", 300f, 350f, 30f, 15f, 12f, 500f),
                new FoodDTO(null, userId, "Salad cá ngừ", "2025-01-01", "Afternoon", loadImage("DefaultFoods/saladcangu.jpg"), 1f, "bowl", 300f, 280f, 20f, 15f, 14f, 400f),
                new FoodDTO(null, userId, "Cơm chiên trứng + kim chi", "2025-01-01", "Afternoon", loadImage("DefaultFoods/comchienkimchi.jpg"), 1f, "plate", 420f, 500f, 14f, 55f, 18f, 600f),
                new FoodDTO(null, userId, "Bún chả", "2025-01-01", "Afternoon", loadImage("DefaultFoods/buncha.jpg"), 1f, "bowl", 450f, 520f, 28f, 40f, 18f, 700f),
                new FoodDTO(null, userId, "Mì Ý sốt bò bằm", "2025-01-01", "Afternoon", loadImage("DefaultFoods/miy.jpg"), 1f, "plate", 400f, 540f, 25f, 45f, 20f, 750f),
                new FoodDTO(null, userId, "Sushi & miso soup", "2025-01-01", "Afternoon", loadImage("DefaultFoods/sushi.jpg"), 1f, "set", 350f, 400f, 20f, 40f, 8f, 500f),
                // Thức ăn gọi ý buổi tối
                new FoodDTO(null, userId, "Lẩu hải sản", "2025-01-01", "Evening", loadImage("DefaultFoods/lauhaisan.jpg"), 1f, "serving", 500f, 480f, 35f, 30f, 18f, 1000f),
                new FoodDTO(null, userId, "Cơm gà luộc + canh cải", "2025-01-01", "Evening", loadImage("DefaultFoods/comgaluoc.jpg"), 1f, "plate", 450f, 520f, 28f, 40f, 16f, 700f),
                new FoodDTO(null, userId, "Cơm thịt kho tiêu", "2025-01-01", "Evening", loadImage("DefaultFoods/thitkhotieu.jpg"), 1f, "plate", 420f, 550f, 26f, 42f, 22f, 850f),
                new FoodDTO(null, userId, "Bún măng vịt", "2025-01-01", "Evening", loadImage("DefaultFoods/bunmangvit.jpg"), 1f, "bowl", 450f, 480f, 30f, 40f, 12f, 900f),
                new FoodDTO(null, userId, "Mì xào bò", "2025-01-01", "Evening", loadImage("DefaultFoods/mixaobo.jpg"), 1f, "plate", 400f, 540f, 22f, 50f, 20f, 750f),
                new FoodDTO(null, userId, "Cá chiên + canh chua", "2025-01-01", "Evening", loadImage("DefaultFoods/cachiencanhchua.jpg"), 1f, "serving", 450f, 500f, 30f, 35f, 18f, 820f),
                new FoodDTO(null, userId, "Salad ức gà + khoai nướng", "2025-01-01", "Evening", loadImage("DefaultFoods/saladga.jpg"), 1f, "plate", 350f, 400f, 32f, 20f, 10f, 450f),
                new FoodDTO(null, userId, "Bánh xèo", "2025-01-01", "Evening", loadImage("DefaultFoods/banhxeo.jpg"), 2f, "pieces", 300f, 460f, 14f, 35f, 25f, 650f),
                new FoodDTO(null, userId, "Cơm cá kho tộ", "2025-01-01", "Evening", loadImage("DefaultFoods/cakho.jpg"), 1f, "plate", 430f, 580f, 28f, 40f, 22f, 870f),
                new FoodDTO(null, userId, "Gà kho gừng + rau luộc", "2025-01-01", "Evening", loadImage("DefaultFoods/gakhogung.jpg"), 1f, "plate", 420f, 520f, 26f, 30f, 18f, 600f),
                new FoodDTO(null, userId, "Bún Thái", "2025-01-01", "Evening", loadImage("DefaultFoods/bunthai.jpg"), 1f, "bowl", 450f, 470f, 20f, 38f, 15f, 780f),
                new FoodDTO(null, userId, "Cơm hến", "2025-01-01", "Evening", loadImage("DefaultFoods/comhen.jpg"), 1f, "bowl", 350f, 400f, 18f, 35f, 10f, 500f),
                new FoodDTO(null, userId, "Miến gà", "2025-01-01", "Evening", loadImage("DefaultFoods/mienga.jpg"), 1f, "bowl", 400f, 380f, 22f, 30f, 8f, 650f),
                new FoodDTO(null, userId, "Canh sườn hầm rau củ", "2025-01-01", "Evening", loadImage("DefaultFoods/suonham.jpg"), 1f, "bowl", 400f, 420f, 28f, 25f, 14f, 600f),
                new FoodDTO(null, userId, "Tôm rim + canh cải", "2025-01-01", "Evening", loadImage("DefaultFoods/tomrim.jpg"), 1f, "plate", 400f, 460f, 30f, 30f, 16f, 750f),
                new FoodDTO(null, userId, "Gỏi gà xé phay + xôi", "2025-01-01", "Evening", loadImage("DefaultFoods/goiga.jpg"), 1f, "plate", 400f, 500f, 28f, 38f, 14f, 680f),
                new FoodDTO(null, userId, "Cá hồi áp chảo + salad", "2025-01-01", "Evening", loadImage("DefaultFoods/cahoi.jpg"), 1f, "plate", 350f, 420f, 32f, 10f, 16f, 500f),
                new FoodDTO(null, userId, "Chả giò + cơm trắng", "2025-01-01", "Evening", loadImage("DefaultFoods/chagio.jpg"), 1f, "plate", 400f, 520f, 22f, 45f, 20f, 700f),
                new FoodDTO(null, userId, "Súp gà + bánh mì nướng", "2025-01-01", "Evening", loadImage("DefaultFoods/supga.jpg"), 1f, "serving", 350f, 400f, 20f, 30f, 10f, 600f),
                new FoodDTO(null, userId, "Bánh canh giò heo", "2025-01-01", "Evening", loadImage("DefaultFoods/banhcanhgion.jpg"), 1f, "bowl", 450f, 520f, 25f, 45f, 18f, 850f),
                // Thức ăn gợi ý buổi snack
                new FoodDTO(null, userId, "Táo", "2025-01-01", "Snack", loadImage("DefaultFoods/apple.jpg"), 1f, "fruit", 182f, 95f, 0.5f, 25f, 0.3f, 2f),
                new FoodDTO(null, userId, "Chuối", "2025-01-01", "Snack", loadImage("DefaultFoods/banana.jpg"), 1f, "fruit", 118f, 105f, 1.3f, 27f, 0.3f, 1f),
                new FoodDTO(null, userId, "Sữa chua không đường", "2025-01-01", "Snack", loadImage("DefaultFoods/yogurt.jpg"), 1f, "cup", 150f, 100f, 10f, 4f, 2f, 60f),
                new FoodDTO(null, userId, "Hạnh nhân", "2025-01-01", "Snack", loadImage("DefaultFoods/almond.jpg"), 1f, "gram", 28f, 160f, 6f, 6f, 14f, 1f),
                new FoodDTO(null, userId, "Phô mai que", "2025-01-01", "Snack", loadImage("DefaultFoods/cheesestick.jpg"), 1f, "piece", 28f, 80f, 6f, 1f, 6f, 200f),
                new FoodDTO(null, userId, "Bắp rang không bơ", "2025-01-01", "Snack", loadImage("DefaultFoods/popcorn.jpg"), 1f, "cup", 8f, 31f, 1f, 6f, 0.3f, 1f),
                new FoodDTO(null, userId, "Cà rốt sống", "2025-01-01", "Snack", loadImage("DefaultFoods/carrot.jpg"), 1f, "stick", 61f, 25f, 0.5f, 6f, 0.1f, 50f),
                new FoodDTO(null, userId, "Bánh gạo", "2025-01-01", "Snack", loadImage("DefaultFoods/ricecake.jpg"), 1f, "piece", 9f, 35f, 0.5f, 7f, 0f, 20f),//
                new FoodDTO(null, userId, "Trứng luộc", "2025-01-01", "Snack", loadImage("DefaultFoods/egg.jpg"), 1f, "egg", 50f, 78f, 6f, 0.6f, 5f, 62f),
                new FoodDTO(null, userId, "Đậu phộng rang", "2025-01-01", "Snack", loadImage("DefaultFoods/peanut.jpg"), 1f, "gram", 30f, 170f, 7f, 6f, 14f, 5f),
                new FoodDTO(null, userId, "Sữa hạt", "2025-01-01", "Snack", loadImage("DefaultFoods/nutmilk.jpg"), 1f, "cup", 240f, 120f, 3f, 10f, 6f, 90f),
                new FoodDTO(null, userId, "Bánh mì sandwich", "2025-01-01", "Snack", loadImage("DefaultFoods/sandwich.jpg"), 1f, "slice", 40f, 100f, 3f, 20f, 1.5f, 190f),
                new FoodDTO(null, userId, "Phô mai tươi", "2025-01-01", "Snack", loadImage("DefaultFoods/cheese.jpg"), 1f, "piece", 20f, 70f, 4f, 0.5f, 6f, 130f),
                new FoodDTO(null, userId, "Hạt điều", "2025-01-01", "Snack", loadImage("DefaultFoods/cashew.jpg"), 1f, "gram", 28f, 155f, 5f, 9f, 12f, 3f),//
                new FoodDTO(null, userId, "Ổi", "2025-01-01", "Snack", loadImage("DefaultFoods/guava.jpg"), 1f, "fruit", 165f, 112f, 4.2f, 23f, 0.9f, 3f),
                new FoodDTO(null, userId, "Bánh quy ngũ cốc", "2025-01-01", "Snack", loadImage("DefaultFoods/granola.jpg"), 1f, "piece", 25f, 130f, 3f, 18f, 5f, 80f),
                new FoodDTO(null, userId, "Thanh protein", "2025-01-01", "Snack", loadImage("DefaultFoods/proteinbar.jpg"), 1f, "bar", 50f, 200f, 15f, 20f, 7f, 150f),
                new FoodDTO(null, userId, "Sữa đậu nành", "2025-01-01", "Snack", loadImage("DefaultFoods/soyamilk.jpg"), 1f, "cup", 240f, 100f, 7f, 8f, 4f, 90f),
                new FoodDTO(null, userId, "Cam", "2025-01-01", "Snack", loadImage("DefaultFoods/orange.jpg"), 1f, "fruit", 130f, 62f, 1.2f, 15.4f, 0.2f, 0f),
                new FoodDTO(null, userId, "Bánh yến mạch", "2025-01-01", "Snack", loadImage("DefaultFoods/oatcookie.jpg"), 1f, "piece", 30f, 140f, 3f, 18f, 6f, 95f)
        );

        for (FoodDTO dto : defaultFoods) {
            createFood(dto); // dùng lại hàm đã có
        }
    }

    private byte[] loadImage(String path) {
        try {
            return new ClassPathResource(path).getInputStream().readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
