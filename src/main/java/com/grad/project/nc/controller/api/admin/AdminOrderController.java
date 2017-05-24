package com.grad.project.nc.controller.api.admin;

import com.grad.project.nc.controller.api.dto.FrontendOrder;
import com.grad.project.nc.model.Address;
import com.grad.project.nc.service.exceptions.IncorrectComplaintStateException;
import com.grad.project.nc.service.exceptions.IncorrectOrderStateException;
import com.grad.project.nc.service.exceptions.IncorrectRoleException;
import com.grad.project.nc.service.orders.OrdersService;
import com.grad.project.nc.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by DeniG on 23.05.2017.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    OrdersService ordersService;
    ProductService productService;

    private static final long ORDER_AIM_CREATE = 13L;

    @Autowired
    public AdminOrderController(OrdersService ordersService, ProductService productService) {
        this.ordersService = ordersService;
        this.productService = productService;
    }

    @RequestMapping(value = "/get/all/size/{size}/offset/{offset}")
    public Collection<FrontendOrder> getOrders(@PathVariable Long size, @PathVariable Long offset) {

        return ordersService.getAllOrders(size, offset).stream().map((item) -> {
            FrontendOrder order = FrontendOrder.fromEntity(item);

            if (item.getOrderAim().getCategoryId().longValue() == ORDER_AIM_CREATE) {
                order.setPossibleDomains(new HashMap<>());
                item.getUser().getDomains().forEach((domain) -> {
                    order.getPossibleDomains().put(domain.getDomainId(), getFullAddress(domain.getAddress()));
                });

                order.setPossibleProducts(new HashMap<>());
                productService.findByProductTypeId(item.getProductInstance().getPrice()
                        .getProduct().getProductType().getProductTypeId())
                        .forEach((product -> order.getPossibleProducts()
                                .put(product.getProductId(), product.getProductName())));
            }

            return order;
        }).collect(Collectors.toList());
    }

    @RequestMapping(value = "/set/responsible", method = RequestMethod.POST)
    public Map<String, Object> setResponsible(@RequestBody Map<String, String> params){
        Long orderId = Long.parseLong(params.get("orderId"));
        Long responsibleId = Long.parseLong(params.get("responsibleId"));

        Map<String, Object> result = new HashMap<>();
        try {
            ordersService.setResponsible(orderId, responsibleId);
            result.put("status" , "success");
            result.put("message" , "New responsible successfully appointed");
        } catch (IncorrectRoleException | IncorrectOrderStateException e) {
            result.put("status" , "error");
            result.put("message" , e.getMessage());

        }
        return result;
    }

    private static String getFullAddress(Address address) {
        String apt = "";
        if (address.getApartmentNumber() != null && !address.getApartmentNumber().isEmpty()) {
            apt = " apt: " + address.getApartmentNumber();
        }
        return address.getLocation().getGooglePlaceId() + apt;
    }

}
