(function () {
    'use strict';

    var OB = window.OldBook;
    var UI = window.AdminUI;
    if (!OB || !UI) {
        return;
    }

    var $ = function (id) { return document.getElementById(id); };

    var API = '/api/admin/reports/revenue';

    // Kỳ dài hơn số ngày này thì biểu đồ gộp theo tháng cho dễ nhìn; bảng và file xuất vẫn theo ngày.
    var MAX_DAILY_BARS = 92;
    var MAX_LABELLED_BARS = 12;

    var METRICS = [
        { key: 'soDon', label: 'Số đơn', plot: 'chartSoDon', format: formatNumber },
        { key: 'gmv', label: 'GMV', plot: 'chartGmv', format: formatMoney },
        { key: 'hoaHong', label: 'Hoa hồng sàn', plot: 'chartHoaHong', format: formatMoney }
    ];
    var BAR_IDLE = 'bg-secondary/70';
    var BAR_ACTIVE = 'bg-secondary';

    var loading = false;
    var report = null;
    // Mỗi phần tử là một cột trên biểu đồ: một ngày, hoặc một tháng khi kỳ dài.
    var buckets = [];
    var activeBucket = -1;

    // =========================================================
    // ĐỊNH DẠNG
    // =========================================================

    function formatNumber(value) {
        return Number(value || 0).toLocaleString('vi-VN');
    }

    function formatMoney(value) {
        return Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 0 }) + ' đ';
    }

    // Dạng rút gọn cho nhãn trục: 200 N, 1,5 Tr
    function formatCompact(value) {
        return Number(value || 0).toLocaleString('vi-VN', { notation: 'compact', maximumFractionDigits: 1 });
    }

    // Ngày từ API có dạng yyyy-MM-dd; tách chuỗi để không bị lệch múi giờ như khi dùng new Date().
    function formatDay(isoDate, withYear) {
        var parts = String(isoDate || '').split('-');
        if (parts.length !== 3) {
            return String(isoDate || '');
        }
        return parts[2] + '/' + parts[1] + (withYear ? '/' + parts[0] : '');
    }

    function pad(value) {
        return (value < 10 ? '0' : '') + value;
    }

    // Ngày theo giờ máy người dùng, dạng yyyy-MM-dd (toISOString dùng giờ UTC nên có thể lệch một ngày).
    function isoDate(date) {
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());
    }

    // =========================================================
    // BỘ LỌC KỲ
    // =========================================================

    function presetRange(preset) {
        var today = new Date();
        var from = new Date(today);
        var to = new Date(today);

        if (preset === '7d') {
            from.setDate(today.getDate() - 6);
        } else if (preset === '30d') {
            from.setDate(today.getDate() - 29);
        } else if (preset === 'month') {
            from = new Date(today.getFullYear(), today.getMonth(), 1);
        } else if (preset === 'last-month') {
            from = new Date(today.getFullYear(), today.getMonth() - 1, 1);
            to = new Date(today.getFullYear(), today.getMonth(), 0);
        }
        return { from: isoDate(from), to: isoDate(to) };
    }

    // preset rỗng nghĩa là người dùng tự nhập ngày: không nút kỳ nhanh nào được chọn.
    function markPreset(preset) {
        var buttons = $('reportPresets').querySelectorAll('[data-preset]');
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].setAttribute('aria-pressed', String(buttons[i].getAttribute('data-preset') === preset));
        }
    }

    function applyPreset(preset) {
        var range = presetRange(preset);
        $('reportFrom').value = range.from;
        $('reportTo').value = range.to;
        markPreset(preset);
        loadReport();
    }

    // =========================================================
    // BIỂU ĐỒ
    // =========================================================

    function toBuckets(points) {
        if (points.length <= MAX_DAILY_BARS) {
            return points.map(function (point) {
                return {
                    label: formatDay(point.ngay),
                    title: 'Ngày ' + formatDay(point.ngay, true),
                    soDon: Number(point.soDon) || 0,
                    gmv: Number(point.gmv) || 0,
                    hoaHong: Number(point.hoaHong) || 0
                };
            });
        }

        var months = [];
        var byMonth = {};
        points.forEach(function (point) {
            var key = String(point.ngay).slice(0, 7);
            if (!byMonth[key]) {
                var label = key.slice(5) + '/' + key.slice(0, 4);
                byMonth[key] = { label: label, title: 'Tháng ' + label, soDon: 0, gmv: 0, hoaHong: 0 };
                months.push(byMonth[key]);
            }
            byMonth[key].soDon += Number(point.soDon) || 0;
            byMonth[key].gmv += Number(point.gmv) || 0;
            byMonth[key].hoaHong += Number(point.hoaHong) || 0;
        });
        return months;
    }

    // Đỉnh trục "tròn": 2, 4 hoặc 10 nhân lũy thừa của 10, để vạch giữa cũng là số tròn.
    function niceMax(value) {
        if (!(value > 0)) {
            return 2;
        }
        var power = Math.pow(10, Math.floor(Math.log10(value)));
        if (power < 1) {
            power = 1;
        }
        var scaled = value / power;
        return (scaled <= 2 ? 2 : scaled <= 4 ? 4 : 10) * power;
    }

    function plotHtml(metric) {
        var values = buckets.map(function (bucket) { return bucket[metric.key]; });
        var max = niceMax(Math.max.apply(null, values));
        var last = values.length - 1;

        var ticks = [max, max / 2, 0].map(function (tick, index) {
            return '<span class="absolute right-0 -translate-y-1/2" style="top: ' + (index * 50) + '%">' +
                OB.escapeHtml(formatCompact(tick)) + '</span>';
        }).join('');

        var grid = '<div class="absolute inset-x-0 top-0 border-t border-surface-container"></div>' +
            '<div class="absolute inset-x-0 top-1/2 border-t border-surface-container"></div>' +
            '<div class="absolute inset-x-0 bottom-0 border-t border-line-border"></div>';

        // Mỗi cột chiếm trọn chiều cao để dễ rê chuột; cột dữ liệu tối đa 24px, hai cột cách nhau 2px.
        var bars = values.map(function (value, index) {
            var height = value > 0 ? Math.max(value / max * 100, 1.5) : 0;
            return '<div class="flex h-full flex-1 items-end justify-center px-px" data-bucket="' + index + '">' +
                '<div data-bar class="w-full max-w-[24px] rounded-t ' + BAR_IDLE + '" style="height: ' + height.toFixed(2) + '%"></div>' +
                '</div>';
        }).join('');

        // Ít cột thì ghi nhãn dưới từng cột; nhiều cột thì chỉ ghi đầu, giữa và cuối cho khỏi chồng chữ.
        var labelEvery = values.length <= MAX_LABELLED_BARS;
        var axis = labelEvery
            ? buckets.map(function (bucket) {
                return '<span class="flex-1 text-center">' + OB.escapeHtml(bucket.label) + '</span>';
            }).join('')
            : [0, Math.floor(last / 2), last].map(function (index) {
                return '<span>' + OB.escapeHtml(buckets[index].label) + '</span>';
            }).join('');

        return '<div class="flex gap-2">' +
            '<div class="relative h-32 w-11 shrink-0 font-caption text-[11px] text-text-muted tabular-nums">' + ticks + '</div>' +
            '<div class="min-w-0 flex-1">' +
            '<div data-plot tabindex="0" role="group" class="relative h-32 rounded outline-none focus-visible:ring-2 focus-visible:ring-focus-glow"' +
            ' aria-label="' + OB.escapeHtml(metric.label) + ' trong kỳ. Dùng phím mũi tên trái, phải để xem từng cột.">' +
            grid +
            '<div class="absolute inset-0 flex items-end">' + bars + '</div>' +
            '</div>' +
            '<div class="mt-1 flex ' + (labelEvery ? '' : 'justify-between ') +
            'font-caption text-[11px] text-text-muted tabular-nums">' + axis + '</div>' +
            '</div>' +
            '</div>';
    }

    function clearActiveBucket() {
        activeBucket = -1;
        $('reportTip').classList.add('hidden');
        paintActiveBucket();
    }

    // Tô đậm cùng một cột trên cả ba biểu đồ.
    function paintActiveBucket() {
        var slots = $('reportCharts').querySelectorAll('[data-bucket]');
        for (var i = 0; i < slots.length; i++) {
            var active = Number(slots[i].getAttribute('data-bucket')) === activeBucket;
            var bar = slots[i].querySelector('[data-bar]');
            bar.classList.toggle(BAR_ACTIVE, active);
            bar.classList.toggle(BAR_IDLE, !active);
        }
    }

    function fillTip(bucket) {
        var tip = $('reportTip');
        tip.textContent = '';

        var title = document.createElement('div');
        title.className = 'font-caption text-caption opacity-75';
        title.textContent = bucket.title;
        tip.appendChild(title);

        METRICS.forEach(function (metric) {
            var row = document.createElement('div');
            row.className = 'flex items-baseline justify-between gap-4';

            var label = document.createElement('span');
            label.className = 'font-caption text-caption opacity-75';
            label.textContent = metric.label;

            var value = document.createElement('strong');
            value.className = 'font-label-lg text-label-lg';
            value.textContent = metric.format(bucket[metric.key]);

            row.appendChild(label);
            row.appendChild(value);
            tip.appendChild(row);
        });
    }

    // slot: cột [data-bucket] đang được rê chuột hoặc chọn bằng bàn phím.
    function showBucket(slot) {
        var index = Number(slot.getAttribute('data-bucket'));
        var bucket = buckets[index];
        if (!bucket) {
            return;
        }

        if (index !== activeBucket) {
            activeBucket = index;
            paintActiveBucket();
            fillTip(bucket);
        }

        var tip = $('reportTip');
        var card = $('reportCharts');
        tip.classList.remove('hidden');

        // Đặt ô chú giải ngay trên đỉnh cột, không để tràn ra ngoài khung biểu đồ.
        var cardBox = card.getBoundingClientRect();
        var slotBox = slot.getBoundingClientRect();
        var barBox = slot.querySelector('[data-bar]').getBoundingClientRect();
        var half = tip.offsetWidth / 2;
        var left = slotBox.left + slotBox.width / 2 - cardBox.left;

        tip.style.left = Math.max(half, Math.min(cardBox.width - half, left)) + 'px';
        tip.style.top = (barBox.top - cardBox.top - 8) + 'px';
    }

    function onChartKey(event) {
        var plot = event.target.closest('[data-plot]');
        var last = buckets.length - 1;
        if (!plot || last < 0) {
            return;
        }

        var next;
        if (event.key === 'ArrowLeft') {
            next = Math.max(0, (activeBucket < 0 ? last : activeBucket) - 1);
        } else if (event.key === 'ArrowRight') {
            next = Math.min(last, activeBucket + 1);
        } else if (event.key === 'Home') {
            next = 0;
        } else if (event.key === 'End') {
            next = last;
        } else {
            return;
        }

        event.preventDefault();
        showBucket(plot.querySelector('[data-bucket="' + next + '"]'));
    }

    // =========================================================
    // HIỂN THỊ BÁO CÁO
    // =========================================================

    function renderTable(data) {
        var cell = '<td class="py-1.5 text-right tabular-nums">';

        $('reportRows').innerHTML = data.points.map(function (point) {
            return '<tr class="border-t border-line-border/60">' +
                '<td class="py-1.5">' + OB.escapeHtml(formatDay(point.ngay, true)) + '</td>' +
                cell + formatNumber(point.soDon) + '</td>' +
                cell + formatMoney(point.gmv) + '</td>' +
                cell + formatMoney(point.hoaHong) + '</td>' +
                '</tr>';
        }).join('');

        $('reportSumSoDon').textContent = formatNumber(data.tongSoDon);
        $('reportSumGmv').textContent = formatMoney(data.tongGmv);
        $('reportSumHoaHong').textContent = formatMoney(data.tongHoaHong);
    }

    function renderReport(data) {
        report = data;
        buckets = toBuckets(data.points || []);
        clearActiveBucket();

        var range = 'Từ ' + formatDay(data.tuNgay, true) + ' đến ' + formatDay(data.denNgay, true) +
            ' (' + formatNumber(data.points.length) + ' ngày)';
        var hasOrders = data.tongSoDon > 0;

        $('reportTotalSoDon').textContent = formatNumber(data.tongSoDon);
        $('reportTotalGmv').textContent = formatMoney(data.tongGmv);
        $('reportTotalHoaHong').textContent = formatMoney(data.tongHoaHong);
        $('reportRange').textContent = range;

        $('reportEmpty').classList.toggle('hidden', hasOrders);
        $('reportCharts').classList.toggle('hidden', !hasOrders);
        $('reportTable').classList.toggle('hidden', !hasOrders);
        $('reportExport').disabled = !hasOrders;
        if (!hasOrders) {
            return;
        }

        $('reportChartNote').textContent = data.points.length > MAX_DAILY_BARS
            ? 'Mỗi cột là một tháng, vì kỳ dài hơn ' + MAX_DAILY_BARS + ' ngày'
            : 'Mỗi cột là một ngày';
        METRICS.forEach(function (metric) {
            $(metric.plot).innerHTML = plotHtml(metric);
        });
        renderTable(data);
    }

    function setLoading(value) {
        loading = value;

        // Khi bị từ chối quyền, layout đã thay toàn bộ <main> nên các phần tử này có thể không còn.
        var submit = $('reportSubmit');
        if (!submit) {
            return;
        }
        submit.disabled = value;
        // Giữ nguyên báo cáo cũ (làm mờ) trong lúc tải, không để khung nhảy.
        $('reportBody').classList.toggle('opacity-50', value);
    }

    function showError(message) {
        $('reportErrorText').textContent = message;
        $('reportError').classList.remove('hidden');
    }

    function loadReport() {
        if (loading) {
            return;
        }

        var from = $('reportFrom').value;
        var to = $('reportTo').value;
        if (!from || !to) {
            showError('Hãy chọn đủ ngày bắt đầu và ngày kết thúc.');
            return;
        }
        if (from > to) {
            showError('Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.');
            return;
        }

        setLoading(true);

        OB.request('GET', API + '?tuNgay=' + encodeURIComponent(from) + '&denNgay=' + encodeURIComponent(to))
            .then(function (data) {
                if (!$('reportSubmit')) {
                    return;
                }
                renderReport(data);
                $('reportError').classList.add('hidden');
            })
            .catch(function (err) {
                if (UI.handleApiError(err) || !$('reportSubmit')) {
                    return;
                }
                // Báo cáo cũ vẫn giữ nguyên trên màn hình, kèm kỳ của nó ở tiêu đề bảng.
                showError((err && err.message) || 'Vui lòng thử lại sau.');
            })
            .then(function () {
                setLoading(false);
            });
    }

    // =========================================================
    // XUẤT CSV
    // =========================================================

    function csvRow(cells) {
        return cells.map(function (cell) {
            return '"' + String(cell == null ? '' : cell).replace(/"/g, '""') + '"';
        }).join(',');
    }

    // Xuất đúng báo cáo đang hiển thị. Ngày dạng yyyy-MM-dd và số không có dấu phân cách để Excel hiểu đúng.
    function exportCsv() {
        if (!report) {
            return;
        }

        var rows = [csvRow(['Ngày giao', 'Số đơn giao thành công', 'GMV (đ)', 'Hoa hồng sàn (đ)'])];
        report.points.forEach(function (point) {
            rows.push(csvRow([point.ngay, point.soDon, point.gmv, point.hoaHong]));
        });
        rows.push(csvRow(['Tổng cộng', report.tongSoDon, report.tongGmv, report.tongHoaHong]));

        UI.downloadFile(
            'platform-revenue_' + report.tuNgay + '_' + report.denNgay + '.csv',
            rows.join('\r\n'),
            'text/csv;charset=utf-8;'
        );
    }

    function init() {
        if (!UI.guard()) {
            return;
        }

        var today = isoDate(new Date());
        $('reportFrom').setAttribute('max', today);
        $('reportTo').setAttribute('max', today);

        $('reportPresets').addEventListener('click', function (event) {
            var button = event.target.closest('[data-preset]');
            if (button) {
                applyPreset(button.getAttribute('data-preset'));
            }
        });

        $('reportFilter').addEventListener('submit', function (event) {
            event.preventDefault();
            markPreset('');
            loadReport();
        });

        $('reportExport').addEventListener('click', exportCsv);

        var charts = $('reportCharts');
        charts.addEventListener('pointermove', function (event) {
            var slot = event.target.closest('[data-bucket]');
            if (slot) {
                showBucket(slot);
            }
        });
        charts.addEventListener('pointerleave', clearActiveBucket);
        // Bàn phím: vào biểu đồ thì chọn cột mới nhất, mũi tên để đổi cột.
        charts.addEventListener('focusin', function (event) {
            var plot = event.target.closest('[data-plot]');
            if (plot && buckets.length) {
                showBucket(plot.querySelector('[data-bucket="' + (buckets.length - 1) + '"]'));
            }
        });
        charts.addEventListener('focusout', clearActiveBucket);
        charts.addEventListener('keydown', onChartKey);

        applyPreset('30d');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
