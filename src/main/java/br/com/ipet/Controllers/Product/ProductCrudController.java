package br.com.ipet.Controllers.Product;

import br.com.ipet.Controllers.File.FileController;
import br.com.ipet.Models.Company;
import br.com.ipet.Models.Order;
import br.com.ipet.Models.Product;
import br.com.ipet.Security.JWT.JwtProvider;
import br.com.ipet.Services.CompanyService;
import br.com.ipet.Services.FileStorageService;
import br.com.ipet.Services.OrderService;
import br.com.ipet.Services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://aw-petcare-client.herokuapp.com",
        "https://aw-petcare-business.herokuapp.com",
        "http://aw-petcare-client.herokuapp.com",
        "http://aw-petcare-business.herokuapp.com",
        "https://petcare-client.vercel.app/",
        "https://petcare-client.vercel.app",
        "https://petcare-business.vercel.app"
})
@RestController
@RequestMapping("/api")
public class ProductCrudController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/products/{page}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Page<Product>> listProducts(@PathVariable("page") int pageNumber, HttpServletRequest req) {
        String jwtToken = jwtProvider.getJwt(req);
        String emailOwnerLogged = jwtProvider.getEmailFromJwtToken(jwtToken);
        if (emailOwnerLogged != null) {
            Company company = companyService.findByOwnerEmail(emailOwnerLogged);
            Pageable pageable = PageRequest.of(pageNumber, 10);
            if (company != null) {
                return ResponseEntity.ok(productService.findAllProducts(company.getProducts(), pageable));
            } else {
                return ResponseEntity.ok(null);
            }
        } else {
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping("/company-products/{id}/{page}")
    public ResponseEntity<Page<Product>> listIdsToProducts(@PathVariable("id") long id, @PathVariable("page") int pageNumber) {
        if (companyService.existsById(id)) {
            Company company = companyService.findById(id);
            Pageable pageable = PageRequest.of(pageNumber, 10);
            return ResponseEntity.ok(productService.findByIds(company.getProducts(), pageable));
        } else {
            return ResponseEntity.ok(null);
        }
    }

    @GetMapping("/order-products/{id}/{page}")
    public ResponseEntity<Page<Product>> listIdsToOrderProducts(@PathVariable("id") long id, @PathVariable("page") int pageNumber) {
        if (orderService.existsById(id)) {
            Order order = orderService.findById(id);
            Pageable pageable = PageRequest.of(pageNumber, 10);
            return ResponseEntity.ok(productService.findByIds(order.getProductsIdsCart(), pageable));
        } else {
            return ResponseEntity.ok(null);
        }
    }

    @PostMapping("/create-product")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<String> saveProduct(MultipartFile file, Product product, HttpServletRequest req) {
        if (product != null) {
            String jwtToken = jwtProvider.getJwt(req);
            String emailOwnerLogged = jwtProvider.getEmailFromJwtToken(jwtToken);

            if (file != null && (file.getOriginalFilename().contains("jpeg") || file.getOriginalFilename().contains("png") || file.getOriginalFilename().contains("jpg"))) {
                String fileName = fileStorageService.storeFile(file);
                Resource resource = fileStorageService.loadFileAsResource(fileName);
                URI contextUrl = URI.create(req.getRequestURL().toString()).resolve(req.getContextPath());
                String urlImage = contextUrl + "images/" + resource.getFilename();
                if (urlImage.contains("/api")) {
                    urlImage = urlImage.replace("/api", "");
                }
                product.setAvatar(urlImage);
            }

            if (!emailOwnerLogged.isEmpty()) {
                productService.save(product);
                Company company = companyService.findByOwnerEmail(emailOwnerLogged);
                company.getProducts().add(product.getId());
                companyService.save(company);
                return ResponseEntity.ok("Product was created");
            } else {
                return ResponseEntity.ok("Any company connected on application");
            }
        } else {
            return ResponseEntity.ok("Product was empty");
        }
    }

    @DeleteMapping("/delete-product/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<String> deleteProduct(@PathVariable long id, HttpServletRequest req) {
        String jwtToken = jwtProvider.getJwt(req);
        String emailOwnerLogged = jwtProvider.getEmailFromJwtToken(jwtToken);

        if (emailOwnerLogged != null) {
            productService.removeById(id);
            return ResponseEntity.ok("Product was deleted successfully");
        } else {
            return ResponseEntity.ok("Owner not loggedin");
        }
    }

    @GetMapping("/products-list/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> findProductById(@PathVariable long id, HttpServletRequest req) {
        String jwtToken = jwtProvider.getJwt(req);
        String emailOwnerLogged = jwtProvider.getEmailFromJwtToken(jwtToken);

        if (emailOwnerLogged != null) {
            if (productService.existsById(id)) {
                return ResponseEntity.ok(productService.findById(id));
            } else {
                return new ResponseEntity<>("Não existe nenhum produto com este id!",
                        HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>("Any company connected on application",
                    HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/edit-product/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<?> editProduct(@PathVariable long id, MultipartFile file, Product product, HttpServletRequest req) {
        if (product != null && productService.existsById(id)) {
            String jwtToken = jwtProvider.getJwt(req);
            String emailOwnerLogged = jwtProvider.getEmailFromJwtToken(jwtToken);

            if (file != null && (file.getOriginalFilename().contains("jpeg") || file.getOriginalFilename().contains("png") || file.getOriginalFilename().contains("jpg"))) {
                String removeContext = "/api/edit-product";
                String urlImage = FileController.saveImage(file, req, removeContext, fileStorageService);
                if (product.getAvatar() == null || !product.getAvatar().contains(urlImage)) {
                    product.setAvatar(urlImage);
                }
            }

            if (!emailOwnerLogged.isEmpty()) {
                productService.save(product);
                return new ResponseEntity<>("Product was edited",
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Any company connected on application",
                        HttpStatus.FORBIDDEN);
            }
        } else {
            return new ResponseEntity<>("Product is empty",
                    HttpStatus.NOT_FOUND);
        }
    }
}
