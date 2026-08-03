package com.example.fishing.controller.merchant;

import com.example.fishing.common.BusinessException;
import com.example.fishing.common.CurrentUser;
import com.example.fishing.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantContentController extends MerchantBaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> announcements(
            @RequestParam(required = false) Long pondId,
            @RequestParam(required = false) String status) {
        Long merchantId = requireMerchantId();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, merchant_id, pond_id, title, content, cover_image, push_home, status, publish_time, create_time, update_time " +
                        "FROM merchant_announcements WHERE merchant_id = ?");
        args.add(merchantId);
        if (pondId != null) {
            checkPondOwner(pondId);
            sql.append(" AND pond_id = ?");
            args.add(pondId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            args.add(status.trim());
        }
        sql.append(" ORDER BY publish_time DESC, id DESC");
        return Result.success(camelRows(jdbcTemplate.queryForList(sql.toString(), args.toArray())));
    }

    @PostMapping("/announcements")
    public Result<Void> createAnnouncement(@RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        Long pondId = longValue(body.get("pondId"));
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        jdbcTemplate.update(
                "INSERT INTO merchant_announcements (merchant_id, pond_id, title, content, cover_image, push_home, status, publish_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                merchantId,
                pondId,
                requiredString(body, "title"),
                stringValue(body.get("content")),
                stringValue(body.get("coverImage")),
                boolInt(body.get("pushHome")),
                defaultString(body.get("status"), "published"),
                timeValue(body.get("publishTime")));
        logAction(merchantId, "create", "announcement", null, requiredString(body, "title"));
        return Result.success();
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_announcements", id, merchantId);
        Long pondId = longValue(body.get("pondId"));
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        jdbcTemplate.update(
                "UPDATE merchant_announcements SET pond_id=?, title=?, content=?, cover_image=?, push_home=?, status=?, publish_time=?, update_time=NOW() WHERE id=? AND merchant_id=?",
                pondId,
                requiredString(body, "title"),
                stringValue(body.get("content")),
                stringValue(body.get("coverImage")),
                boolInt(body.get("pushHome")),
                defaultString(body.get("status"), "published"),
                timeValue(body.get("publishTime")),
                id,
                merchantId);
        logAction(merchantId, "update", "announcement", id, requiredString(body, "title"));
        return Result.success();
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        Long merchantId = requireMerchantId();
        jdbcTemplate.update("DELETE FROM merchant_announcements WHERE id=? AND merchant_id=?", id, merchantId);
        logAction(merchantId, "delete", "announcement", id, null);
        return Result.success();
    }

    @GetMapping("/events")
    public Result<List<Map<String, Object>>> events(@RequestParam(required = false) String status) {
        Long merchantId = requireMerchantId();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, merchant_id, pond_id, title, cover_image, event_time, signup_deadline, capacity, location, introduction, " +
                        "audit_enabled, cancel_rule, form_fields, signup_count, status, recommended, pinned, create_time, update_time " +
                        "FROM merchant_events WHERE merchant_id = ?");
        args.add(merchantId);
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            args.add(status.trim());
        }
        sql.append(" ORDER BY pinned DESC, event_time DESC, id DESC");
        return Result.success(camelRows(jdbcTemplate.queryForList(sql.toString(), args.toArray())));
    }

    @PostMapping("/events")
    public Result<Void> createEvent(@RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        Long pondId = longValue(body.get("pondId"));
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        jdbcTemplate.update(
                "INSERT INTO merchant_events (merchant_id, pond_id, title, cover_image, event_time, signup_deadline, capacity, location, introduction, audit_enabled, cancel_rule, form_fields, status, recommended, pinned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                merchantId,
                pondId,
                requiredString(body, "title"),
                stringValue(body.get("coverImage")),
                timeValue(body.get("eventTime")),
                timeValue(body.get("signupDeadline")),
                intValue(body.get("capacity"), 0),
                stringValue(body.get("location")),
                stringValue(body.get("introduction")),
                boolInt(body.get("auditEnabled")),
                stringValue(body.get("cancelRule")),
                stringValue(body.get("formFields")),
                defaultString(body.get("status"), "published"),
                boolInt(body.get("recommended")),
                boolInt(body.get("pinned")));
        logAction(merchantId, "create", "event", null, requiredString(body, "title"));
        return Result.success();
    }

    @PutMapping("/events/{id}")
    public Result<Void> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", id, merchantId);
        Long pondId = longValue(body.get("pondId"));
        if (pondId != null) {
            checkPondOwner(pondId);
        }
        jdbcTemplate.update(
                "UPDATE merchant_events SET pond_id=?, title=?, cover_image=?, event_time=?, signup_deadline=?, capacity=?, location=?, introduction=?, audit_enabled=?, cancel_rule=?, form_fields=?, status=?, recommended=?, pinned=?, update_time=NOW() WHERE id=? AND merchant_id=?",
                pondId,
                requiredString(body, "title"),
                stringValue(body.get("coverImage")),
                timeValue(body.get("eventTime")),
                timeValue(body.get("signupDeadline")),
                intValue(body.get("capacity"), 0),
                stringValue(body.get("location")),
                stringValue(body.get("introduction")),
                boolInt(body.get("auditEnabled")),
                stringValue(body.get("cancelRule")),
                stringValue(body.get("formFields")),
                defaultString(body.get("status"), "published"),
                boolInt(body.get("recommended")),
                boolInt(body.get("pinned")),
                id,
                merchantId);
        logAction(merchantId, "update", "event", id, requiredString(body, "title"));
        return Result.success();
    }

    @PutMapping("/events/{id}/status")
    public Result<Void> updateEventFlags(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", id, merchantId);
        jdbcTemplate.update(
                "UPDATE merchant_events SET status=COALESCE(?, status), recommended=COALESCE(?, recommended), pinned=COALESCE(?, pinned), update_time=NOW() WHERE id=? AND merchant_id=?",
                nullableString(body.get("status")),
                body.containsKey("recommended") ? boolInt(body.get("recommended")) : null,
                body.containsKey("pinned") ? boolInt(body.get("pinned")) : null,
                id,
                merchantId);
        logAction(merchantId, "update-status", "event", id, null);
        return Result.success();
    }

    @DeleteMapping("/events/{id}")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        Long merchantId = requireMerchantId();
        jdbcTemplate.update("DELETE FROM merchant_event_reviews WHERE event_id=? AND merchant_id=?", id, merchantId);
        jdbcTemplate.update("DELETE FROM merchant_event_signups WHERE event_id=? AND merchant_id=?", id, merchantId);
        jdbcTemplate.update("DELETE FROM merchant_events WHERE id=? AND merchant_id=?", id, merchantId);
        logAction(merchantId, "delete", "event", id, null);
        return Result.success();
    }

    @GetMapping("/events/{eventId}/signups")
    public Result<List<Map<String, Object>>> eventSignups(@PathVariable Long eventId) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        String sql = "SELECT id, event_id, merchant_id, user_id, user_name, phone, form_data, audit_status, audit_reason, checked_in, create_time, update_time " +
                "FROM merchant_event_signups WHERE event_id=? AND merchant_id=? ORDER BY create_time DESC";
        return Result.success(camelRows(jdbcTemplate.queryForList(sql, eventId, merchantId)));
    }

    @PutMapping("/events/{eventId}/signups/{signupId}/audit")
    public Result<Void> auditSignup(@PathVariable Long eventId, @PathVariable Long signupId, @RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        jdbcTemplate.update(
                "UPDATE merchant_event_signups SET audit_status=?, audit_reason=?, update_time=NOW() WHERE id=? AND event_id=? AND merchant_id=?",
                defaultString(body.get("auditStatus"), "approved"),
                stringValue(body.get("auditReason")),
                signupId,
                eventId,
                merchantId);
        logAction(merchantId, "audit", "event-signup", signupId, null);
        return Result.success();
    }

    @PutMapping("/events/{eventId}/signups/{signupId}/checkin")
    public Result<Void> checkinSignup(@PathVariable Long eventId, @PathVariable Long signupId, @RequestBody Map<String, Object> body) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        jdbcTemplate.update(
                "UPDATE merchant_event_signups SET checked_in=?, update_time=NOW() WHERE id=? AND event_id=? AND merchant_id=?",
                boolInt(body.get("checkedIn")),
                signupId,
                eventId,
                merchantId);
        logAction(merchantId, "checkin", "event-signup", signupId, null);
        return Result.success();
    }

    @DeleteMapping("/events/{eventId}/signups")
    public Result<Void> clearSignups(@PathVariable Long eventId) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        jdbcTemplate.update("DELETE FROM merchant_event_signups WHERE event_id=? AND merchant_id=?", eventId, merchantId);
        jdbcTemplate.update("UPDATE merchant_events SET signup_count=0 WHERE id=? AND merchant_id=?", eventId, merchantId);
        logAction(merchantId, "clear", "event-signup", eventId, null);
        return Result.success();
    }

    @GetMapping("/events/{eventId}/reviews")
    public Result<List<Map<String, Object>>> eventReviews(@PathVariable Long eventId) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        String sql = "SELECT id, event_id, merchant_id, user_id, user_name, rating, content, create_time " +
                "FROM merchant_event_reviews WHERE event_id=? AND merchant_id=? ORDER BY create_time DESC";
        return Result.success(camelRows(jdbcTemplate.queryForList(sql, eventId, merchantId)));
    }

    @DeleteMapping("/events/{eventId}/reviews/{reviewId}")
    public Result<Void> deleteReview(@PathVariable Long eventId, @PathVariable Long reviewId) {
        Long merchantId = requireMerchantId();
        ensureOwner("merchant_events", eventId, merchantId);
        jdbcTemplate.update("DELETE FROM merchant_event_reviews WHERE id=? AND event_id=? AND merchant_id=?", reviewId, eventId, merchantId);
        logAction(merchantId, "delete", "event-review", reviewId, null);
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> logs(
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long merchantId = requireMerchantId();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, merchant_id, operator_id, operator_name, action_type, target_type, target_id, ip, detail, create_time " +
                        "FROM merchant_operation_logs WHERE merchant_id = ?");
        args.add(merchantId);
        if (operatorName != null && !operatorName.trim().isEmpty()) {
            sql.append(" AND operator_name LIKE ?");
            args.add("%" + operatorName.trim() + "%");
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append(" AND create_time >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append(" AND create_time <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }
        sql.append(" ORDER BY create_time DESC LIMIT 500");
        return Result.success(camelRows(jdbcTemplate.queryForList(sql.toString(), args.toArray())));
    }

    private void ensureOwner(String table, Long id, Long merchantId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + table + " WHERE id=? AND merchant_id=?",
                Integer.class,
                id,
                merchantId);
        if (count == null || count == 0) {
            throw new BusinessException("resource not found");
        }
    }

    private void logAction(Long merchantId, String action, String targetType, Long targetId, String detail) {
        CurrentUser.Context ctx = CurrentUser.getContext();
        Long operatorId = ctx == null ? null : ctx.getUserId();
        String operatorName = ctx != null && ctx.getStaffRole() != null ? ctx.getStaffRole() : "owner";
        jdbcTemplate.update(
                "INSERT INTO merchant_operation_logs (merchant_id, operator_id, operator_name, action_type, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?, ?, ?)",
                merchantId,
                operatorId,
                operatorName,
                action,
                targetType,
                targetId,
                detail);
    }

    private List<Map<String, Object>> camelRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Timestamp) {
                    value = value.toString().substring(0, 19);
                }
                item.put(camel(entry.getKey()), value);
            }
            result.add(item);
        }
        return result;
    }

    private String camel(String key) {
        String lower = key.toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String requiredString(Map<String, Object> body, String key) {
        String value = stringValue(body.get(key));
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(key + " required");
        }
        return value.trim();
    }

    private String defaultString(Object value, String fallback) {
        String str = stringValue(value);
        return str == null || str.trim().isEmpty() ? fallback : str.trim();
    }

    private String nullableString(Object value) {
        String str = stringValue(value);
        return str == null || str.trim().isEmpty() ? null : str.trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return fallback;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Integer boolInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        String str = String.valueOf(value);
        return "1".equals(str) || "true".equalsIgnoreCase(str) || "on".equalsIgnoreCase(str) ? 1 : 0;
    }

    private Object timeValue(Object value) {
        String str = stringValue(value);
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        str = str.trim().replace('T', ' ');
        if (str.length() == 16) {
            str += ":00";
        }
        return Timestamp.valueOf(LocalDateTime.parse(str.replace(" ", "T")));
    }
}
