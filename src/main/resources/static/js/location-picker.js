/*
 * Ô chọn Tỉnh/Thành phố và Phường/Xã dùng chung (đăng ký cửa hàng, sửa thông tin cửa hàng).
 * Danh sách lấy từ /api/locations và được nhớ lại trong trang để không gọi lặp.
 *
 * Cách dùng:
 *   var picker = OldBook.locations.attach(provinceSelect, wardSelect);
 *   picker.ready                 -> Promise, xong khi đã nạp danh sách tỉnh
 *   picker.setValue(maTinh, maXa) -> Promise, chọn sẵn (dùng khi sửa thông tin)
 *   picker.value()               -> { maTinh: Number|null, maXa: Number|null }
 */
(function () {
  'use strict';

  var OB = window.OldBook;
  if (!OB) { return; }

  var provinceCache = null;
  var wardCache = {};

  function loadProvinces() {
    if (!provinceCache) {
      provinceCache = OB.request('GET', '/api/locations/provinces').then(function (list) {
        return list || [];
      }, function (err) {
        provinceCache = null;
        throw err;
      });
    }
    return provinceCache;
  }

  function loadWards(maTinh) {
    var key = String(maTinh);
    if (!wardCache[key]) {
      wardCache[key] = OB.request('GET', '/api/locations/provinces/' + encodeURIComponent(maTinh) + '/wards')
        .then(function (list) {
          return list || [];
        }, function (err) {
          delete wardCache[key];
          throw err;
        });
    }
    return wardCache[key];
  }

  function fill(select, placeholder, items, valueKey, labelKey) {
    select.innerHTML = '';
    var first = document.createElement('option');
    first.value = '';
    first.textContent = placeholder;
    select.appendChild(first);
    items.forEach(function (item) {
      var option = document.createElement('option');
      option.value = String(item[valueKey]);
      option.textContent = item[labelKey];
      select.appendChild(option);
    });
  }

  function attach(provinceSelect, wardSelect) {
    var wardRequest = 0;

    function resetWards(placeholder) {
      fill(wardSelect, placeholder, [], 'maXa', 'tenXa');
      wardSelect.disabled = true;
    }

    function showWards(maTinh, selectedXa) {
      var sequence = ++wardRequest;
      if (!maTinh) {
        resetWards('Chọn tỉnh/thành phố trước');
        return Promise.resolve();
      }
      resetWards('Đang tải phường/xã...');
      return loadWards(maTinh).then(function (wards) {
        if (sequence !== wardRequest) { return; }
        if (!wards.length) {
          resetWards('Chưa có dữ liệu phường/xã cho tỉnh này');
          return;
        }
        fill(wardSelect, 'Chọn phường/xã', wards, 'maXa', 'tenXa');
        wardSelect.disabled = false;
        if (selectedXa) { wardSelect.value = String(selectedXa); }
      }, function (err) {
        if (sequence !== wardRequest) { return; }
        resetWards('Không tải được phường/xã');
        throw err;
      });
    }

    resetWards('Chọn tỉnh/thành phố trước');
    fill(provinceSelect, 'Đang tải tỉnh/thành phố...', [], 'maTinh', 'tenTinh');
    provinceSelect.disabled = true;

    var ready = loadProvinces().then(function (provinces) {
      fill(provinceSelect, 'Chọn tỉnh/thành phố', provinces, 'maTinh', 'tenTinh');
      provinceSelect.disabled = false;
    }, function (err) {
      fill(provinceSelect, 'Không tải được tỉnh/thành phố', [], 'maTinh', 'tenTinh');
      throw err;
    });

    provinceSelect.addEventListener('change', function () {
      showWards(provinceSelect.value).catch(function () { /* đã hiện trong ô chọn */ });
    });

    return {
      ready: ready,
      setValue: function (maTinh, maXa) {
        return ready.then(function () {
          provinceSelect.value = maTinh ? String(maTinh) : '';
          return showWards(provinceSelect.value, maXa);
        });
      },
      value: function () {
        return {
          maTinh: provinceSelect.value ? Number(provinceSelect.value) : null,
          maXa: wardSelect.value ? Number(wardSelect.value) : null
        };
      }
    };
  }

  OB.locations = { attach: attach, provinces: loadProvinces, wards: loadWards };
})();
