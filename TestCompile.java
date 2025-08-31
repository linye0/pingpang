// 简单测试编译
import com.pingpang.training.dto.BookingRequest;

public class TestCompile {
    public static void main(String[] args) {
        BookingRequest request = new BookingRequest();
        String notes = request.getNotes(); // 这行应该不会报错
        System.out.println("getNotes() method exists: " + (notes != null ? "YES" : "YES"));
    }
}