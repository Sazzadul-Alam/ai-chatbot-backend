package com.ds.tracks.user.daoImpl;

import com.ds.tracks.commons.models.PagedResponseRequest;
import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.user.dao.UserDao;
import com.ds.tracks.user.dao.UserPermissionDao;
import com.ds.tracks.user.model.User;
import com.ds.tracks.user.model.UsersPermission;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.ds.tracks.commons.utils.UserStatus.ACTIVE;
import static com.ds.tracks.commons.utils.UserStatus.ONBOARDING;
import static com.ds.tracks.commons.utils.Utils.*;
import static java.util.stream.Collectors.toMap;

@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public User save(User user) {
        return mongoTemplate.save(user);
    }

    @Override
    public UpdateResult update(User user) {
        BasicQuery basicQuery=new BasicQuery("{\"_id\":ObjectId(\""+user.getId()+"\")}");
        Update update=new Update()
                .set("loginId",user.getLoginId())
                .set("fullName",user.getFullName());
        UpdateResult updateResult=mongoTemplate.updateFirst(basicQuery,update,"user");
        return updateResult;
    }

    @Override
    public UpdateResult activate(String loginId,String otp) {
        Query query = new Query(Criteria.where("loginId").is(loginId).and("otp").is(otp));
        Update update=new Update().set("status",ACTIVE);
        UpdateResult updateResult = mongoTemplate.updateFirst(query,update,"user");
        return updateResult;
    }

    @Override
    public UpdateResult invitedPrivilegeCreate(String loginId,String userId) {
        Query query = new Query(Criteria.where("loginId").is(loginId));
        Update update=new Update().set("userId",userId);
        UpdateResult updateResult = mongoTemplate.updateMulti(query,update,CollectionName.users_permission);
        return updateResult;
    }

    @Override
    public int deleteById(String id) {
        return 0;
    }

    @Override
    public User findById(String id) {
        return null;
    }

    @Override
    public User findByLoginId(String loginId) {
        Query query = new Query(Criteria.where("loginId").is(loginId));
        List<User> userList=mongoTemplate.find(query,User.class, "user");
        return (userList.size()>0)?userList.get(0):new User();
    }


    @Override
    public List<UserDao> findAll(Integer page, Integer size) {
        return null;
    }
    @Override
    public List<User> findByLogidIdAndStatus(String loginId,String status){
        Query query = new Query(Criteria.where("loginId").is(loginId).and("status").is(status));
        return mongoTemplate.find(query,User.class, "user");
    }

    @Override
    public Map<String, String> findActiveUsersByLoginId(String username){
        Query query = new Query(Criteria.where("loginId").is(username).and("status").is("ACTIVE"));
        query.fields().include("loginId","password","fullName");
        return mongoTemplate.find(query, User.class, "user").stream()
                .collect(toMap(User::getLoginId, User::getPassword, (a, b) -> b));

    }

    @Override
    public User findUserByLoginId(String username){
        Query query = new Query(Criteria.where("loginId").is(username).and("status").is("ACTIVE"));
        query.fields().include("loginId","password","fullName");
        List<User> user=mongoTemplate.find(query, User.class, "user");
        return (user.size()>0)?user.get(0):null;

    }

    @Override
    public Map<String, String> findActiveUsers(){
        Query query = new Query(Criteria.where("status").is("ACTIVE"));
        query.fields().include("id","fullName");
        return mongoTemplate.find(query, User.class, "user").stream()
                .collect(toMap(User::getId, User::getFullName, (a, b) -> b));

    }

    @Override
    public UpdateResult otpReset(String loginId, String otp, Date otpValidationTime) {
        Query query = new Query(Criteria.where("loginId").is(loginId));
        Update update=new Update().set("otp",otp).set("otpValidationTime",otpValidationTime);
        UpdateResult updateResult=mongoTemplate.updateFirst(query,update,"user");
        return updateResult;
    }

    @Override
    public Map<String, String> findUsersByLoginIds(List<String> username){
        Query query = new Query(Criteria.where("loginId").in(username));
        query.fields().include("loginId","_id");
        return mongoTemplate.find(query, User.class).stream()
                .collect(toMap(User::getLoginId, User::getId, (a, b) -> b));
    }

    @Override
    public void updateStatus(String userId, String newStatus) {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(userId)), Update.update("status", newStatus), User.class);
    }

    @Override
    public UpdateResult update(String imageString, String name) {
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        Update update = new Update();
        if(isValidString(name)){
            update.set("fullName", name);
        }
        if(isValidString(imageString)){
            update.set("image", imageString);
        }
        return mongoTemplate.updateFirst(Query.query(Criteria.where("loginId").is(loginId)), update, User.class);
    }

    @Override
    public void updatePassword(String name, String password) {
        mongoTemplate.updateFirst(new Query(Criteria.where("loginId").is(name)), new Update().set("password", password), User.class);
    }

    @Override
    public List<Document> getUsersByIds(List<String> userIds) {
        List<Document> res =new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(userIds));
        query.fields().include("fullName", "image", "loginId");
        mongoTemplate.find(query, User.class).forEach(u-> res.add(new Document("name", u.getFullName()).append("image", u.getImage()).append("email", u.getLoginId()).append("id",u.getId())));
        return  res;

    }

    @Override
    public Boolean hasPermission(String spaceId, String loginId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("loginId").is(loginId).and("spaceId").is(spaceId).and("role").is("ADMIN"));
        query.fields().include("_id");

        return mongoTemplate.exists(query, UsersPermission.class);
    }

    @Override
    public boolean hasWSPermission(String workspaceId, String loginId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("loginId").is(loginId).and("workspaceId").is(workspaceId).and("role").is("ADMIN"));
        query.fields().include("_id");

        return mongoTemplate.exists(query, UsersPermission.class);
    }

    @Override
    public List<User> getUsersByEmails(List<String> email) {
        Query query = new Query();
        query.addCriteria(Criteria.where("loginId").in(email));
        query.fields().include("id", "loginId");

        return mongoTemplate.find(query, User.class);
    }

    @Override
    public List<Document> findAllUserIdAndNameAndImage() {
        final String query =  "{ aggregate: '"+CollectionName.user+"'," +
                " pipeline:[\n" +
                "    {$match:{ loginId:{ $ne:'support@datasoft-bd.com' }, status:'ACTIVE' }},\n" +
                "    {$project:{\n" +
                "        name:'$fullName', \n" +
                "        image:1, \n" +
                "        id:{$toString:'$_id'}, \n" +
                "        _id:0\n" +
                "    }}," +
                "   { $sort:{ name:1 } }\n" +
                "], allowDiskUse: true, cursor: { batchSize: 20000000000 }}";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");

    }

    @Override
    public Object getAllUsersForWorkspace(String id) {
        final String query =  "{ aggregate: '"+CollectionName.user+"'," +
                " pipeline:[\n" +
                "    {$match:{ loginId:{ $ne:'support@datasoft-bd.com' }}},\n" +
                "    {$project:{\n" +
                "        name:'$fullName', \n" +
                "        image:1, \n" +
                "        email:'$loginId', \n" +
                "        status:1, \n" +
                "        designation:1, \n" +
                "        access:1, \n" +
                "        role:1, \n" +
                "        id:{$toString:'$_id'}, \n" +
                "        _id:0\n" +
                "    }}," +
                "   { $sort:{ name:1 } }\n" +
                "], allowDiskUse: true, cursor: { batchSize: 20000000000 }}";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");

    }

    @Override
    public void updateUser(User user) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(user.getId()));
        Update update=new Update()
                .set("fullName",user.getFullName())
                .set("role",user.getRole())
                .set("designation",user.getDesignation())
                .set("status",user.getStatus())
                .set("access", user.getAccess());

        if(isValidString(user.getImage())){
            String image = user.getImage().split("base64,")[1];
            if(image.endsWith(")")){
                image = image.substring(0, image.length() - 1);
            }
            update.set("image",image);
        } else {
            user.setImage(null);
        }
        mongoTemplate.updateFirst(query,update,User.class);
    }

    @Override
    public List<String> getAccess(String loginId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("loginId").is(loginId));
        query.fields().include("access");
        User user = mongoTemplate.findOne(query, User.class);
        return Objects.nonNull(user) ? user.getAccess() : null;
    }

    @Override
    public Object getAllUsersWithNames() {
        Query query = new Query();
        query.addCriteria(Criteria.where("loginId").ne("support@datasoft-bd.com"));
        query.fields().include("fullName");
        query.with(Sort.by(Sort.Order.asc("fullName")));
        return mongoTemplate.find(query, User.class).stream().map((user)-> {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getFullName());
            return userMap;
        }).collect(Collectors.toList());
    }

    @Override
    public Object findAllActiveUsers() {
        final String query =  "{ aggregate: '"+CollectionName.user+"'," +
                " pipeline:[\n" +
                "    { $match:{ status:'ACTIVE', loginId:{ $ne:'support@datasoft-bd.com' } } },\n" +
                "    { $project:{_id:0, fullName:1, id:{ $toString:'$_id' }} },\n" +
                "    { $sort:{ fullName:1 } },\n" +
                "    { $group:{_id:'', data:{ $mergeObjects: { $arrayToObject: [  [{ k: '$id', v: { name:'$fullName' }  }]  ]} }}},\n" +
                "    { $replaceRoot:{ newRoot:'$data' } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? Collections.EMPTY_MAP : res.get(0);
    }
}
