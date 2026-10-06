package com.superfercho.identity.infrastructure.rest;

import com.superfercho.identity.application.dto.ActivateAdminCustomerAccountCommand;
import com.superfercho.identity.application.dto.DeactivateAdminCustomerAccountCommand;
import com.superfercho.identity.application.dto.FindAdminCustomerByDocumentCommand;
import com.superfercho.identity.application.dto.GetAdminCustomerRecordCommand;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.dto.GetAdminNewCustomersCommand;
import com.superfercho.identity.application.dto.ListAdminCustomerCommercialHistoryCommand;
import com.superfercho.identity.application.usecase.SearchAdminCustomerRecordsUseCase;
import com.superfercho.identity.infrastructure.rest.dto.AdminCustomerRecordsPageRestResponse;
import com.superfercho.identity.application.usecase.ActivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.DeactivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.FindAdminCustomerByDocumentUseCase;
import com.superfercho.identity.application.usecase.GetAdminCustomerRecordUseCase;
import com.superfercho.identity.application.usecase.GetAdminNewCustomersUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerOrdersUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerPaymentsUseCase;
import com.superfercho.identity.infrastructure.rest.dto.AdminCustomerAccountRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.AdminCustomerOrdersRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.AdminCustomerPaymentsRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.AdminCustomerRecordRestResponse;
import com.superfercho.identity.infrastructure.rest.dto.AdminNewCustomersRestResponse;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {

    private final FindAdminCustomerByDocumentUseCase findAdminCustomerByDocumentUseCase;
    private final GetAdminCustomerRecordUseCase getAdminCustomerRecordUseCase;
    private final ListAdminCustomerOrdersUseCase listAdminCustomerOrdersUseCase;
    private final ListAdminCustomerPaymentsUseCase listAdminCustomerPaymentsUseCase;
    private final ActivateAdminCustomerAccountUseCase activateAdminCustomerAccountUseCase;
    private final DeactivateAdminCustomerAccountUseCase deactivateAdminCustomerAccountUseCase;
    private final GetAdminNewCustomersUseCase getAdminNewCustomersUseCase;
    private final SearchAdminCustomerRecordsUseCase searchAdminCustomerRecordsUseCase;

    public AdminCustomerController(
            FindAdminCustomerByDocumentUseCase findAdminCustomerByDocumentUseCase,
            GetAdminCustomerRecordUseCase getAdminCustomerRecordUseCase,
            ListAdminCustomerOrdersUseCase listAdminCustomerOrdersUseCase,
            ListAdminCustomerPaymentsUseCase listAdminCustomerPaymentsUseCase,
            ActivateAdminCustomerAccountUseCase activateAdminCustomerAccountUseCase,
            DeactivateAdminCustomerAccountUseCase deactivateAdminCustomerAccountUseCase,
            GetAdminNewCustomersUseCase getAdminNewCustomersUseCase,
            SearchAdminCustomerRecordsUseCase searchAdminCustomerRecordsUseCase) {
        this.findAdminCustomerByDocumentUseCase = findAdminCustomerByDocumentUseCase;
        this.getAdminCustomerRecordUseCase = getAdminCustomerRecordUseCase;
        this.listAdminCustomerOrdersUseCase = listAdminCustomerOrdersUseCase;
        this.listAdminCustomerPaymentsUseCase = listAdminCustomerPaymentsUseCase;
        this.activateAdminCustomerAccountUseCase = activateAdminCustomerAccountUseCase;
        this.deactivateAdminCustomerAccountUseCase = deactivateAdminCustomerAccountUseCase;
        this.getAdminNewCustomersUseCase = getAdminNewCustomersUseCase;
        this.searchAdminCustomerRecordsUseCase = searchAdminCustomerRecordsUseCase;
    }

    /**
     * New customers for an arbitrary [from, to) period, based on the real
     * registration (CustomerRecord creation) instant, bucketed in America/Bogota.
     */
    @GetMapping("/dashboard/new")
    public AdminNewCustomersRestResponse newCustomers(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String granularity) {
        return AdminNewCustomersRestResponse.from(
                getAdminNewCustomersUseCase.execute(GetAdminNewCustomersCommand.of(from, to, granularity)));
    }

    @GetMapping("/by-document")
    public AdminCustomerRecordRestResponse findByDocument(
            @RequestParam String documentType,
            @RequestParam String documentNumber,
            @RequestParam(required = false) String accountStatus) {
        return AdminCustomerRecordRestResponse.from(findAdminCustomerByDocumentUseCase.execute(
                FindAdminCustomerByDocumentCommand.of(documentType, documentNumber, accountStatus)));
    }

    /**
     * Listado principal «Todos los clientes»: búsqueda, filtro de estado de
     * cuenta, filtro de compras ({@code hasPurchases}), ordenamiento y
     * paginación. Los registrados recientemente se obtienen con
     * {@code sortBy=CREATED_AT&sortDir=DESC}.
     */
    @GetMapping
    public AdminCustomerRecordsPageRestResponse list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String hasPurchases,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return AdminCustomerRecordsPageRestResponse.from(searchAdminCustomerRecordsUseCase.execute(
                AdminCustomerRecordSearchCriteria.of(
                        search, status, sortBy, sortDir, hasPurchases, page, size)));
    }

    @GetMapping("/{customerRecordId}")
    public AdminCustomerRecordRestResponse get(
            @PathVariable UUID customerRecordId, @RequestParam(required = false) String accountStatus) {
        return AdminCustomerRecordRestResponse.from(getAdminCustomerRecordUseCase.execute(
                GetAdminCustomerRecordCommand.of(customerRecordId, accountStatus)));
    }

    @GetMapping("/{customerRecordId}/orders")
    public AdminCustomerOrdersRestResponse listOrders(
            @PathVariable UUID customerRecordId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return AdminCustomerOrdersRestResponse.from(listAdminCustomerOrdersUseCase.execute(
                new ListAdminCustomerCommercialHistoryCommand(customerRecordId, page, size)));
    }

    @GetMapping("/{customerRecordId}/payments")
    public AdminCustomerPaymentsRestResponse listPayments(
            @PathVariable UUID customerRecordId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return AdminCustomerPaymentsRestResponse.from(listAdminCustomerPaymentsUseCase.execute(
                new ListAdminCustomerCommercialHistoryCommand(customerRecordId, page, size)));
    }

    @PostMapping("/{customerRecordId}/accounts/{userId}/activate")
    public AdminCustomerAccountRestResponse activate(
            @PathVariable UUID customerRecordId, @PathVariable UUID userId) {
        return AdminCustomerAccountRestResponse.from(activateAdminCustomerAccountUseCase.execute(
                new ActivateAdminCustomerAccountCommand(customerRecordId, userId)));
    }

    @PostMapping("/{customerRecordId}/accounts/{userId}/deactivate")
    public AdminCustomerAccountRestResponse deactivate(
            @PathVariable UUID customerRecordId, @PathVariable UUID userId) {
        return AdminCustomerAccountRestResponse.from(deactivateAdminCustomerAccountUseCase.execute(
                new DeactivateAdminCustomerAccountCommand(customerRecordId, userId)));
    }
}
