package com.oldbook.repository.system;

import com.oldbook.entity.system.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemLogRepository
        extends JpaRepository<SystemLog, Integer> {

    Optional<SystemLog>
    findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
            String loaiDoiTuong,
            Integer maDoiTuong,
            String hanhDong
    );


    /*
     * =========================================================
     * Lấy 20 hoạt động gần đây của một người dùng
     * =========================================================
     */
    List<SystemLog>
    findTop20ByTaiKhoan_NguoiDung_MaNDOrderByThoiGianDescMaNhatKyDesc(
            Integer maND
    );


    /*
     * =========================================================
     * Tìm kiếm nhật ký quản trị
     * =========================================================
     *
     * keyword:
     * - hành động
     * - loại đối tượng
     * - mô tả
     * - IP
     * - họ tên người thực hiện
     * - email người thực hiện
     *
     * numericKeyword:
     * - mã nhật ký
     * - mã đối tượng
     * - mã tài khoản thực hiện
     * - mã người dùng thực hiện
     */

    @Query(
            value = """
                    SELECT n
                    FROM SystemLog n
                    JOIN n.taiKhoan tk
                    JOIN tk.nguoiDung nd
                    WHERE
                        (
                            :keyword = ''
                            OR LOWER(COALESCE(n.hanhDong, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(n.loaiDoiTuong, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(CAST(n.moTa AS String), ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(n.diaChiIp, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(nd.hoTen, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(nd.email, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND
                        (
                            :numericKeyword IS NULL
                            OR n.maNhatKy = :numericKeyword
                            OR n.maDoiTuong = :numericKeyword
                            OR tk.maTK = :numericKeyword
                            OR nd.maND = :numericKeyword
                        )

                        AND
                        (
                            :hanhDong IS NULL
                            OR :hanhDong = ''
                            OR n.hanhDong = :hanhDong
                        )

                        AND
                        (
                            :vaiTro IS NULL
                            OR :vaiTro = ''
                            OR tk.vaiTro = :vaiTro
                        )

                        AND
                        (
                            :diaChiIp IS NULL
                            OR :diaChiIp = ''
                            OR n.diaChiIp LIKE CONCAT('%', :diaChiIp, '%')
                        )

                        AND
                        (
                            :tuNgay IS NULL
                            OR n.thoiGian >= :tuNgay
                        )

                        AND
                        (
                            :denNgay IS NULL
                            OR n.thoiGian < :denNgay
                        )

                    ORDER BY
                        n.thoiGian DESC,
                        n.maNhatKy DESC
                    """,
            countQuery = """
                    SELECT COUNT(n)
                    FROM SystemLog n
                    JOIN n.taiKhoan tk
                    JOIN tk.nguoiDung nd
                    WHERE
                        (
                            :keyword = ''
                            OR LOWER(COALESCE(n.hanhDong, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(n.loaiDoiTuong, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(CAST(n.moTa AS String), ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(n.diaChiIp, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(nd.hoTen, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))

                            OR LOWER(COALESCE(nd.email, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND
                        (
                            :numericKeyword IS NULL
                            OR n.maNhatKy = :numericKeyword
                            OR n.maDoiTuong = :numericKeyword
                            OR tk.maTK = :numericKeyword
                            OR nd.maND = :numericKeyword
                        )

                        AND
                        (
                            :hanhDong IS NULL
                            OR :hanhDong = ''
                            OR n.hanhDong = :hanhDong
                        )

                        AND
                        (
                            :vaiTro IS NULL
                            OR :vaiTro = ''
                            OR tk.vaiTro = :vaiTro
                        )

                        AND
                        (
                            :diaChiIp IS NULL
                            OR :diaChiIp = ''
                            OR n.diaChiIp LIKE CONCAT('%', :diaChiIp, '%')
                        )

                        AND
                        (
                            :tuNgay IS NULL
                            OR n.thoiGian >= :tuNgay
                        )

                        AND
                        (
                            :denNgay IS NULL
                            OR n.thoiGian < :denNgay
                        )
                    """
    )
    Page<SystemLog> searchAdminLogs(
            @Param("keyword") String keyword,
            @Param("numericKeyword") Integer numericKeyword,
            @Param("hanhDong") String hanhDong,
            @Param("vaiTro") String vaiTro,
            @Param("diaChiIp") String diaChiIp,
            @Param("tuNgay") LocalDateTime tuNgay,
            @Param("denNgay") LocalDateTime denNgay,
            Pageable pageable
    );


    /*
     * =========================================================
     * KPI
     * =========================================================
     */

    long countByThoiGianGreaterThanEqual(
            LocalDateTime tuNgay
    );


    long countByHanhDongAndThoiGianGreaterThanEqual(
            String hanhDong,
            LocalDateTime tuNgay
    );


    @Query("""
            SELECT COUNT(DISTINCT n.diaChiIp)
            FROM SystemLog n
            WHERE n.diaChiIp IS NOT NULL
              AND n.diaChiIp <> ''
              AND n.thoiGian >= :tuNgay
            """)
    long countDistinctIpSince(
            @Param("tuNgay") LocalDateTime tuNgay
    );


    /*
     * Khóa + mở khóa trong ngày.
     */
    @Query("""
            SELECT COUNT(n)
            FROM SystemLog n
            WHERE n.hanhDong IN (
                'KHOA_TAI_KHOAN',
                'MO_KHOA_TAI_KHOAN'
            )
            AND n.thoiGian >= :tuNgay
            """)
    long countLockActionsSince(
            @Param("tuNgay") LocalDateTime tuNgay
    );
}