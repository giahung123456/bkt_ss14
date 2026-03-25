import java.sql.*;



public class SafeTransferDemo {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bkt_ss14?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "123456";

        String senderId = "ACC01";
        String receiverId = "ACC02";
        double amount = 1000;

        Connection conn = null;
        try {

            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Kết nối thành công!");

            conn.setAutoCommit(false);

            String checkBalanceSql = "SELECT Balance FROM Accounts WHERE AccountId = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkBalanceSql)) {
                ps.setString(1, senderId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    throw new SQLException("Tài khoản người gửi không tồn tại!");
                }

                double balance = rs.getDouble("Balance");
                if (balance < amount) {
                    throw new SQLException("Số dư không đủ để chuyển khoản!");
                }
            }

            try (CallableStatement cs = conn.prepareCall("{call sp_UpdateBalance(?, ?)}")) {

                cs.setString(1, senderId);
                cs.setBigDecimal(2, new java.math.BigDecimal(-amount));
                cs.execute();

                cs.setString(1, receiverId);
                cs.setBigDecimal(2, new java.math.BigDecimal(amount));
                cs.execute();
            }

            conn.commit();
            System.out.println("Chuyển khoản thành công!");

            String showResultSql = "SELECT AccountId, FullName, Balance FROM Accounts WHERE AccountId IN (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(showResultSql)) {
                ps.setString(1, senderId);
                ps.setString(2, receiverId);
                ResultSet rs = ps.executeQuery();

                System.out.println("===== Kết quả sau chuyển khoản =====");
                while (rs.next()) {
                    System.out.println(rs.getString("AccountId") + " - "
                            + rs.getString("FullName") + " - "
                            + rs.getBigDecimal("Balance"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Có lỗi xảy ra, rollback giao dịch...");
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
