package br.com.ipet.Controllers.Company;

import br.com.ipet.Models.Company;
import br.com.ipet.Security.JWT.JwtProvider;
import br.com.ipet.Services.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
public class CompanyActionsController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private JwtProvider jwtProvider;

    @PutMapping("/change-company-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<String> getProfileCompanyLogged(HttpServletRequest req) {
        String tokenJWT = jwtProvider.getJwt(req);
        if (tokenJWT != null) {
            String emailOwner = jwtProvider.getEmailFromJwtToken(tokenJWT);
            Company company = companyService.findByOwnerEmail(emailOwner);
            if (company.getStatus().equalsIgnoreCase("Aberto")) {
                company.setStatus("Fechado");
            } else {
                company.setStatus("Aberto");
            }

            companyService.save(company);
            return ResponseEntity.ok("Status was changed sucessfully");
        } else {
            return ResponseEntity.ok("Something went wrong during the change of company's status");
        }
    }

    @PostMapping("/validate-is-open")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<Boolean> checkIfCompanyIsOpen(@RequestBody String cnpj) {
        if (cnpj != null) {
            Company company = companyService.findByCnpj(cnpj);
            if (company.getStatus().equalsIgnoreCase("Aberto")) {
                return ResponseEntity.ok(true);
            } else {
                return ResponseEntity.ok(false);
            }
        } else {
            return ResponseEntity.ok(false);
        }
    }
}
