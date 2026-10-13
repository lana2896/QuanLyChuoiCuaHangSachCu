(function () {
    'use strict';


    var OB = window.OldBook;

    if (!OB) {
        console.error('OldBook chưa được khởi tạo.');
        return;
    }


    var Auth = OB.auth;

    var BASE_URL = '/api/profile/addresses';


    // =========================
    // DOM
    // =========================

    var addressList = document.getElementById('addressList');
    var addressLoading = document.getElementById('addressLoading');
    var addressEmpty = document.getElementById('addressEmpty');
    var addressAlert = document.getElementById('addressAlert');

    var addressModalElement = document.getElementById('addressModal');

    var addressForm = document.getElementById('addressForm');

    var modalTitle = document.getElementById('addressModalTitle');

    var addressId = document.getElementById('addressId');

    var receiverInput = document.getElementById('addressReceiver');
    var phoneInput = document.getElementById('addressPhone');

    var provinceInput = document.getElementById('addressProvince');
    var districtInput = document.getElementById('addressDistrict');
    var wardInput = document.getElementById('addressWard');

    var detailInput = document.getElementById('addressDetail');

    var defaultInput = document.getElementById('addressDefault');

    var saveAddressBtn = document.getElementById('saveAddressBtn');

    var addAddressBtn = document.getElementById('addAddressBtn');


    var addressModal = new bootstrap.Modal(addressModalElement);


    var addresses = [];


    // =========================
    // ALERT
    // =========================

    function showAlert(message, type) {

        addressAlert.className =
            'alert alert-' + type;

        addressAlert.textContent = message;
        addressAlert.classList.remove('d-none');


        setTimeout(function () {

            addressAlert.classList.add('d-none');

        }, 4000);
    }


    function hideAlert() {

        addressAlert.classList.add('d-none');

    }


    // =========================
    // AUTH ERROR
    // =========================

    function handleAuthError(error) {

        if (error && error.status === 401) {

            Auth.clear();

            window.location.replace('/login');

            return true;
        }

        return false;
    }


    // =========================
    // LOAD
    // =========================

    function loadAddresses() {

        addressLoading.classList.remove('d-none');
        addressEmpty.classList.add('d-none');
        addressList.classList.add('d-none');

        OB.request('GET', BASE_URL)

            .then(function (data) {

                addresses = Array.isArray(data)
                    ? data
                    : [];

                renderAddresses();

            })

            .catch(function (error) {

                if (handleAuthError(error)) {
                    return;
                }

                showAlert(
                    error.message || 'Không thể tải danh sách địa chỉ.',
                    'danger'
                );

            })

            .finally(function () {

                addressLoading.classList.add('d-none');

            });
    }


    // =========================
    // RENDER LIST
    // =========================

    function renderAddresses() {

        addressList.innerHTML = '';


        if (addresses.length === 0) {

            addressList.classList.add('d-none');
            addressEmpty.classList.remove('d-none');

            return;
        }


        addressEmpty.classList.add('d-none');
        addressList.classList.remove('d-none');


        addresses.forEach(function (address) {

            addressList.appendChild(
                createAddressCard(address)
            );

        });
    }


    // =========================
    // ADDRESS CARD
    // =========================

    function createAddressCard(address) {

        var card = document.createElement('div');

        card.className =
            'address-card' +
            (address.laMacDinh ? ' default' : '');


        // HEADER
        var header = document.createElement('div');

        header.className =
            'd-flex flex-wrap justify-content-between gap-2 mb-3';


        var receiver = document.createElement('div');

        receiver.className =
            'd-flex flex-wrap align-items-center gap-2';


        var receiverName = document.createElement('strong');

        receiverName.textContent =
            address.tenNguoiNhan || '';


        receiver.appendChild(receiverName);


        var separator = document.createElement('span');

        separator.className = 'text-muted';

        separator.textContent = '|';


        receiver.appendChild(separator);


        var phone = document.createElement('span');

        phone.textContent =
            address.soDienThoai || '';

        phone.className = 'text-muted';


        receiver.appendChild(phone);


        header.appendChild(receiver);


        if (address.laMacDinh) {

            var badge = document.createElement('span');

            badge.className =
                'address-default-badge';

            badge.innerHTML =
                '<i class="bi bi-check-circle"></i>' +
                ' Mặc định';

            header.appendChild(badge);
        }


        card.appendChild(header);


        // ADDRESS
        var addressText = document.createElement('div');

        addressText.className =
            'mb-3 text-muted';


        addressText.textContent =
            [
                address.diaChiChiTiet,
                address.phuongXa,
                address.quanHuyen,
                address.tinhThanh
            ]
                .filter(function (value) {
                    return value && value.trim();
                })
                .join(', ');


        card.appendChild(addressText);


        // ACTIONS
        var actions = document.createElement('div');

        actions.className =
            'address-actions';


        // SET DEFAULT
        if (!address.laMacDinh) {

            var defaultButton =
                document.createElement('button');

            defaultButton.type = 'button';

            defaultButton.className =
                'btn btn-sm btn-outline-brown';

            defaultButton.innerHTML =
                '<i class="bi bi-check2-circle me-1"></i>' +
                'Đặt mặc định';


            defaultButton.addEventListener(
                'click',
                function () {

                    setDefaultAddress(
                        address.maDiaChi
                    );

                }
            );


            actions.appendChild(defaultButton);
        }


        // EDIT
        var editButton =
            document.createElement('button');

        editButton.type = 'button';

        editButton.className =
            'btn btn-sm btn-outline-secondary';

        editButton.innerHTML =
            '<i class="bi bi-pencil me-1"></i>' +
            'Sửa';


        editButton.addEventListener(
            'click',
            function () {

                openEditModal(address);

            }
        );


        actions.appendChild(editButton);


        // DELETE
        var deleteButton =
            document.createElement('button');

        deleteButton.type = 'button';

        deleteButton.className =
            'btn btn-sm btn-outline-danger';

        deleteButton.innerHTML =
            '<i class="bi bi-trash me-1"></i>' +
            'Xóa';


        deleteButton.addEventListener(
            'click',
            function () {

                deleteAddress(
                    address.maDiaChi
                );

            }
        );


        actions.appendChild(deleteButton);


        card.appendChild(actions);


        return card;
    }


    // =========================
    // RESET FORM
    // =========================

    function resetForm() {

        addressForm.reset();

        addressId.value = '';

        modalTitle.textContent =
            'Thêm địa chỉ nhận hàng';

        saveAddressBtn.innerHTML =
            '<i class="bi bi-check2-circle me-1"></i>' +
            'Lưu địa chỉ';


        clearValidation();
    }


    // =========================
    // OPEN CREATE MODAL
    // =========================

    function openCreateModal() {

        resetForm();

        addressModal.show();
    }


    addAddressBtn.addEventListener(
        'click',
        openCreateModal
    );


    // =========================
    // OPEN EDIT MODAL
    // =========================

    function openEditModal(address) {

        addressId.value =
            address.maDiaChi;

        receiverInput.value =
            address.tenNguoiNhan || '';

        phoneInput.value =
            address.soDienThoai || '';

        provinceInput.value =
            address.tinhThanh || '';

        districtInput.value =
            address.quanHuyen || '';

        wardInput.value =
            address.phuongXa || '';

        detailInput.value =
            address.diaChiChiTiet || '';

        defaultInput.checked =
            Boolean(address.laMacDinh);


        modalTitle.textContent =
            'Chỉnh sửa địa chỉ';

        saveAddressBtn.innerHTML =
            '<i class="bi bi-check2-circle me-1"></i>' +
            'Cập nhật địa chỉ';


        clearValidation();

        addressModal.show();
    }


    // =========================
    // VALIDATE
    // =========================

    function validateForm() {

        clearValidation();


        var receiver =
            receiverInput.value.trim();

        var phone =
            phoneInput.value.trim();

        var province =
            provinceInput.value.trim();

        var district =
            districtInput.value.trim();

        var ward =
            wardInput.value.trim();

        var detail =
            detailInput.value.trim();


        if (!receiver) {

            receiverInput.classList.add('is-invalid');

            receiverInput.focus();

            return false;
        }


        if (!phone || !/^0\d{9,10}$/.test(phone)) {

            phoneInput.classList.add('is-invalid');

            phoneInput.focus();

            return false;
        }


        if (!province) {

            provinceInput.classList.add('is-invalid');

            provinceInput.focus();

            return false;
        }


        if (!district) {

            districtInput.classList.add('is-invalid');

            districtInput.focus();

            return false;
        }


        if (!ward) {

            wardInput.classList.add('is-invalid');

            wardInput.focus();

            return false;
        }


        if (!detail) {

            detailInput.classList.add('is-invalid');

            detailInput.focus();

            return false;
        }


        return true;
    }


    function clearValidation() {

        var inputs =
            addressForm.querySelectorAll(
                '.is-invalid'
            );


        inputs.forEach(function (input) {

            input.classList.remove(
                'is-invalid'
            );

        });
    }


    // =========================
    // SAVE
    // =========================

    addressForm.addEventListener(
        'submit',
        function (event) {

            event.preventDefault();


            if (!validateForm()) {
                return;
            }


            var id =
                addressId.value.trim();


            var payload = {

                tenNguoiNhan:
                    receiverInput.value.trim(),

                soDienThoai:
                    phoneInput.value.trim(),

                tinhThanh:
                    provinceInput.value.trim(),

                quanHuyen:
                    districtInput.value.trim(),

                phuongXa:
                    wardInput.value.trim(),

                diaChiChiTiet:
                    detailInput.value.trim(),

                laMacDinh:
                    defaultInput.checked
            };


            setSaveLoading(true);


            var request;


            if (id) {

                request = OB.request(
                    'PUT',
                    BASE_URL + '/' +
                    encodeURIComponent(id),
                    payload
                );

            } else {

                request = OB.request(
                    'POST',
                    BASE_URL,
                    payload
                );
            }


            request

                .then(function () {

                    addressModal.hide();

                    resetForm();

                    loadAddresses();


                    showAlert(
                        id
                            ? 'Cập nhật địa chỉ thành công.'
                            : 'Thêm địa chỉ thành công.',
                        'success'
                    );

                })

                .catch(function (error) {

                    if (handleAuthError(error)) {
                        return;
                    }

                    showAlert(
                        error.message ||
                        'Không thể lưu địa chỉ.',
                        'danger'
                    );

                })

                .finally(function () {

                    setSaveLoading(false);

                });
        }
    );


    // =========================
    // SET DEFAULT
    // =========================

    function setDefaultAddress(id) {

        OB.request(
            'PATCH',
            BASE_URL + '/' +
            encodeURIComponent(id) +
            '/default'
        )

            .then(function () {

                loadAddresses();

                showAlert(
                    'Đã đặt địa chỉ mặc định.',
                    'success'
                );

            })

            .catch(function (error) {

                if (handleAuthError(error)) {
                    return;
                }

                showAlert(
                    error.message ||
                    'Không thể đặt địa chỉ mặc định.',
                    'danger'
                );

            });
    }


    // =========================
    // DELETE
    // =========================

    function deleteAddress(id) {

        var address =
            addresses.find(function (item) {

                return String(item.maDiaChi) ===
                    String(id);

            });


        if (!address) {
            return;
        }


        var message;

        if (address.laMacDinh) {

            message =
                'Đây là địa chỉ mặc định. ' +
                'Bạn có chắc muốn xóa không?';

        } else {

            message =
                'Bạn có chắc muốn xóa địa chỉ này không?';
        }


        if (!window.confirm(message)) {
            return;
        }


        OB.request(
            'DELETE',
            BASE_URL + '/' +
            encodeURIComponent(id)
        )

            .then(function () {

                loadAddresses();

                showAlert(
                    'Đã xóa địa chỉ.',
                    'success'
                );

            })

            .catch(function (error) {

                if (handleAuthError(error)) {
                    return;
                }

                showAlert(
                    error.message ||
                    'Không thể xóa địa chỉ.',
                    'danger'
                );

            });
    }


    // =========================
    // LOADING BUTTON
    // =========================

    function setSaveLoading(loading) {

        saveAddressBtn.disabled =
            loading;


        if (loading) {

            saveAddressBtn.innerHTML =
                '<span class="spinner-border ' +
                'spinner-border-sm me-2"></span>' +
                'Đang lưu...';

        } else {

            var editing =
                Boolean(addressId.value);

            saveAddressBtn.innerHTML =
                editing
                    ? '<i class="bi bi-check2-circle me-1"></i>Cập nhật địa chỉ'
                    : '<i class="bi bi-check2-circle me-1"></i>Lưu địa chỉ';
        }
    }


    // =========================
    // MODAL EVENTS
    // =========================

    addressModalElement.addEventListener(
        'hidden.bs.modal',
        function () {

            resetForm();

        }
    );


    // =========================
    // START
    // =========================

    loadAddresses();

})();