package com.oldbook.controller.location;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.location.ProvinceResponse;
import com.oldbook.dto.location.WardResponse;
import com.oldbook.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceResponse>> getProvinces() {
        return ApiResponse.success(locationService.getProvinces());
    }

    @GetMapping("/provinces/{maTinh}/wards")
    public ApiResponse<List<WardResponse>> getWards(@PathVariable Integer maTinh) {
        return ApiResponse.success(locationService.getWards(maTinh));
    }
}
