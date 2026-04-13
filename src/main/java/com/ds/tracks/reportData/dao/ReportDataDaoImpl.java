package com.ds.tracks.reportData.dao;

import com.ds.tracks.commons.utils.CollectionName;
import com.ds.tracks.commons.utils.JsonDateTimeConverter;
import com.ds.tracks.effort.model.EffortLog;
import com.ds.tracks.reportData.model.ReportData;
import com.ds.tracks.reportData.model.dto.ReportDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import javax.print.Doc;
import java.beans.Expression;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ds.tracks.commons.utils.Utils.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class ReportDataDaoImpl implements ReportDataDao{

    private final MongoTemplate mongoTemplate;

    @Override
    public Object getInvoices(String workspaceId, String sortBy, Integer sortOrder, Integer page, Integer size) {
        String query="{ aggregate: '"+ CollectionName.invoice_data +"', \n" +
                "pipeline: [" +
                "   { $facet:{ " +
                "       data:[" +
                "           { $sort:{ '"+sortBy+"':"+sortOrder+" } },\n" +
                "           { $skip: "+(page*size)+" },\n" +
                "           { $project:{ \n" +
                "               _id:0,\n" +
                "               id:{ $toString:'$_id' },\n" +
                "               invoiceNo:1,\n" +
                "               total:1,\n" +
                "               legacy:1,\n" +
                "               invoiceDate:{ $dateToString: {\n" +
                "                 date: { $toDate:'$_id'  },\n" +
                "                 format: '%d-%m-%Y',\n" +
                "                 onNull: ''\n" +
                "               } },\n" +
                "               clientName:1\n" +
                "           } },\n" +
                "           { $limit:"+size+" }," +

                "       ]," +
                "       metaData: [ { $count: 'total' } ]" +
                "   } }," +
                "   { $unwind:{ path:'$metaData' } }," +
                "   { $project:{ data:1, totalData:'$metaData.total' } } " +
                "], allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        List<?> res = (List<?>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
        return  res.isEmpty() ? noDataResponse() : res.get(0);
    }

    @Override
    public List<Document> clientwiseTasksReport(ReportDto request, String clientType) {
        final String query = "{ aggregate: '"+ CollectionName.task +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ " +
                "       startDate: "+ dateRange(request.getStartDateString(), request.getEndDateString())+ ",\n"+
                        listToStringQuery("spaceId", request.getProject())+
                "    } },\n"+
                "    { $project:{ spaceId:1, subSpaceId:1, _id:0, id:{ $toString:'$_id' }, name:1, status:1, startDate:1 } },\n" +
                "    { $lookup:{ \n" +
                "        from:'"+ CollectionName.spaces +"', \n" +
                "        as:'space', \n" +
                "        let:{ id:'$spaceId' },\n" +
                "        pipeline:[\n" +
                "            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1, clientType:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] }, clientType:'"+clientType+"' } }  \n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$space' },\n" +
                "    { $lookup:{ \n" +
                "        from:'"+ CollectionName.task_schedule +"', \n" +
                "        as:'user', \n" +
                "        let:{ id:'$id' },\n" +
                "        pipeline:[\n" +
                "            { $project:{ _id:0, taskId:1, assignedTo:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$taskId', '$$id' ] } } },\n" +
                "            { $group:{ _id:'$assignedTo'} },\n" +
                "            { $lookup:{ \n" +
                "                from:'"+ CollectionName.user +"', \n" +
                "                as:'user', \n" +
                "                let:{ id:'$_id' },\n" +
                "                pipeline:[\n" +
                "                    { $project:{ _id:0, id:{ $toString:'$_id' }, fullName:1 } },\n" +
                "                    { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }  \n" +
                "                ]\n" +
                "            }},\n" +
                "            { $unwind:'$user' },\n" +
                "            { $replaceRoot:{ newRoot:'$user' } } \n" +
                "        ]\n" +
                "    }},\n" +
                "    { $project: {\n" +
                "        space: '$space.name',\n" +
                "        date:{$dateToString: { format: \"%Y-%m-%d\",  date: \"$startDate\" }},\n" +
                "        task:'$name',\n" +
                "        status:1,\n" +
                "        assigned: { $reduce: { input: '$user', initialValue: '', in: { $concat:[ '$$value', { $cond: [{$eq: ['$$value', '']}, '', ', '] }, '$$this.fullName']}}},\n" +
                "    }},\n" +
                "    { $group:{ \n" +
                "        _id:'$space',\n" +
                "        data:{ $push:'$$ROOT' }\n" +
                "       \n" +
                "    } },\n" +
                "    { $project:{ _id:0, space:'$_id', data:1 } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<Document> invoiceReport(ReportDto request) {
        String invoiceNoFilter = "";
        if(isValidString(request.getInvoiceType())){
            invoiceNoFilter = "invoiceNo:/"+request.getInvoiceType().trim().toUpperCase()+"\\//, ";
        }
        final String query = "{ aggregate: '"+ CollectionName.invoice_data +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ \n" +
                "       invoiceDate:"+ dateRange(request.getStartDateString(), request.getEndDateString())+  ", \n"+
                        invoiceNoFilter +
                        listToStringQuery("clientName", request.getProject())+
                "    } },\n"+
                "    { $project:{\n" +
                "        _id:0,\n" +
                "        client:'$clientName', \n" +
                "        invoiceDate:1,\n" +
                "        date:{ $dateToString: { format: \"%d-%m-%Y\", date: '$invoiceDate'}},\n" +
                "        vat: "+formatCurrency("vatAmount")+"\n" +
                "        subtotal: "+formatCurrency("subtotal")+"\n" +
                "        total: "+formatCurrency("total")+"\n" +
                "        type: { $switch: { \n" +
                "            branches: [\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/ACC\\// }}, then: 'Accounting' },\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/FIN\\// }}, then: 'Finance' },\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/TAX\\// }}, then: 'Tax' },\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/BPO\\// }}, then: 'BPO' },\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/OTH\\// }}, then: 'Others' },\n" +
                "                { case: { $regexMatch: { input: '$invoiceNo', regex: /\\/ACC\\// }}, then: 'Accounting' }\n" +
                "            ], default: '' \n" +
                "        }}\n" +
                "    }},\n" +
                "    { $sort:{ client:1, invoiceDate:1 } },\n" +
                "    { $group:{ _id:'$client', data:{ $push:'$$ROOT' } } },\n" +
                "    { $project:{ _id:0, space:'$_id', data:1 } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }


    private String formatCurrency(String filed){
        return  "{ $concat: [ \n" +
                "   { \n" +
                "    $let: {\n" +
                "        vars: {\n" +
                "            amountStr: { $toString: { $toInt:'$"+filed+"' } },\n" +
                "            numLength: { $strLenCP: { $toString: { $toInt:'$"+filed+"' } } },\n" +
                "        },\n" +
                "        in: {\n" +
                "            $concat: [\n" +
                "                { $cond: [\n" +
                "                    { $gte: [ \"$$numLength\", 7 ] },\n" +
                "                    { $concat: [\n" +
                "                        { $substrCP: [\n" +
                "                            \"$$amountStr\", \n" +
                "                            0, \n" +
                "                            { $max: [ { $subtract: [ \"$$numLength\", 6 ] }, 0 ] }\n" +
                "                        ]},\n" +
                "                        \",\"\n" +
                "                    ]},\n" +
                "                    \"\"\n" +
                "                ]},\n" +
                "                { $substrCP: [\n" +
                "                    \"$$amountStr\",\n" +
                "                    { $max: [ \n" +
                "                        { $cond: [\n" +
                "                            { $gte: [ \"$$numLength\", 7 ] },\n" +
                "                            { $subtract: [ \"$$numLength\", 6 ] },\n" +
                "                            0\n" +
                "                        ]}, \n" +
                "                        0 \n" +
                "                    ]},\n" +
                "                    { $max: [ \n" +
                "                        { $cond: [\n" +
                "                            { $gte: [ \"$$numLength\", 7 ] },\n" +
                "                            { $subtract: [ \"$$numLength\", 5 ] },\n" +
                "                            { $subtract: [ \"$$numLength\", 3 ] }\n" +
                "                        ]},\n" +
                "                        0\n" +
                "                    ]}\n" +
                "                ]},\n" +
                "                { $cond: [ { $lte: [ \"$$numLength\", 3 ] }, \"\", \",\" ]},\n" +
                "                { $substrCP: [ \"$$amountStr\", { $max: [ { $subtract: [ \"$$numLength\", 3 ] }, 0 ] }, 3 ] }\n" +
                "            ]\n" +
                "        }\n" +
                "    }\n" +
                "}]},\n";
    }

    public List<Document> invoiceReportOld(ReportDto request) {
        String invoiceNoFilter = "";
        if(isValidString(request.getInvoiceType())){
            invoiceNoFilter = "invoiceNo:/TAX\\//, ";
        }
        final String query = "{ aggregate: '"+ CollectionName.invoice_data +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ invoiceDate:"+ dateRange(request.getStartDateString(), request.getEndDateString())+  ",\n"+
                invoiceNoFilter +
                listToStringQuery("clientName", request.getProject())+"} },\n"+
                "    { $project:{ vatAmount:1, subtotal:1, total:1, clientName:1, year:{$year:'$invoiceDate' }, month:{$month:'$invoiceDate' } } },\n" +
                "    { $group:{ _id:{ client:'$clientName', year:'$year', month:'$month'}, vat:{ $sum:'$vatAmount' }, subtotal:{ $sum:'$subtotal' }, total:{ $sum:'$total' } } },\n" +
                "    { $project:{ _id:0, \n" +
                "        client:'$_id.client', \n" +
                "        year:{ $toString:'$_id.year' },\n" +
                "        month: '$_id.month',  \n" +
                "        vat: { $toString:'$vat' },\n" +
                "        subtotal: { $toString:'$subtotal' }, \n" +
                "        total: { $toString:'$total' },\n" +
                "        monthName: { $switch: {\n" +
                "            branches: [\n" +
                "                { case: { $eq: [\"$_id.month\", 1] }, then: \"January\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 2] }, then: \"February\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 3] }, then: \"March\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 4] }, then: \"April\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 5] }, then: \"May\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 6] }, then: \"June\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 7] }, then: \"July\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 8] }, then: \"August\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 9] }, then: \"September\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 10] }, then: \"October\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 11] }, then: \"November\" },\n" +
                "                { case: { $eq: [\"$_id.month\", 12] }, then: \"December\" }\n" +
                "            ], \n" +
                "            default: \"\" \n" +
                "        }} \n" +
                "    }},\n" +
                "    { $sort:{ client:1, year:1, month:1 } },\n" +
                "    { $group:{ _id:'$client', data:{ $push:'$$ROOT' } } },\n" +
                "    { $project:{ _id:0, space:'$_id', data:1 } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }


    @Override
    public List<Document> personalTaxReport(ReportDto request) {
        final String query = "{ aggregate: '"+ CollectionName.task +"', \n" +
                "pipeline: [\n" +
                "    { $match:{ " +
                "       startDate: "+ dateRange(request.getStartDateString(), request.getEndDateString())+ ",\n"+
                listToStringQuery("spaceId", request.getProject())+
                "    } },\n"+
                "    { $project:{ spaceId:1, _id:0, id:{ $toString:'$_id' }, name:1, status:1, startDate:1 } },\n" +
                "    { $lookup:{ \n" +
                "        from:'spaces', \n" +
                "        as:'space', \n" +
                "        let:{ id:'$spaceId' },\n" +
                "        pipeline:[\n" +
                "            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1, clientType:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] }, clientType:'Personal Tax' } }  \n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:'$space' },\n" +
                "    { $lookup:{ \n" +
                "        from:'task_schedule', \n" +
                "        as:'user', \n" +
                "        let:{ id:'$id' },\n" +
                "        pipeline:[\n" +
                "            { $project:{ _id:0, taskId:1, assignedTo:1 } },\n" +
                "            { $match:{ $expr:{ $eq:[ '$taskId', '$$id' ] } } },\n" +
                "            { $group:{ _id:'$assignedTo'} },\n" +
                "            { $lookup:{ \n" +
                "                from:'user', \n" +
                "                as:'user', \n" +
                "                let:{ id:'$_id' },\n" +
                "                pipeline:[\n" +
                "                    { $project:{ _id:0, id:{ $toString:'$_id' }, fullName:1 } },\n" +
                "                    { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }  \n" +
                "                ]\n" +
                "            }},\n" +
                "            { $unwind:'$user' },\n" +
                "            { $replaceRoot:{ newRoot:'$user' } },\n" +
                "            { $lookup:{\n" +
                "               from:'effort_log',\n" +
                "               as:'effort',\n" +
                "               let:{ id:'$id', taskId:'$$id'},\n" +
                "               pipeline:[\n" +
                "                   { $match:{ $expr:{ $and:[ { $eq:['$taskId', '$$taskId']  }, { $eq:['$createdBy', '$$id']  } ] } } },\n" +
                "                   { $group:{ _id:'', duration:{ $sum:'$duration' } } }\n" +
                "               ]\n" +
                "           }},\n" +
                "           { $unwind:{ path:'$effort', preserveNullAndEmptyArrays:true } },\n" +
                "           { $project:{ name:'$fullName', duration:'$effort.duration' } }\n" +
                "        ]\n" +
                "    }},\n" +
                "    { $unwind:{ path:'$user', preserveNullAndEmptyArrays:true } }," +
                "    { $project:{ space:'$space.name', task:'$name', date:{$dateToString: { format: \"%Y-%m-%d\",  date: \"$startDate\" }}, status:1, assigned:'$user.name', duration:{ $toString:{ $ifNull:['$user.duration', 0] } } } }, \n" +
                "    { $sort:{ space:1, date:-1 } },\n" +
                "    { $group:{ _id:'$space', data:{ $push:'$$ROOT' } } },\n" +
                "    { $project:{ _id:0, space:'$_id', data:1 } }\n" +
                "] allowDiskUse: true, cursor: {batchSize: 20000000000} }";
        return (List<Document>) ((Document) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

// Grouped Tasks
//    { $project:{ spaceId:1, subSpaceId:1, _id:0, id:{ $toString:'$_id' }, name:1, status:1, startDate:1 } },
//    { $lookup:{
//        from:'spaces',
//        as:'space',
//        let:{ id:'$spaceId' },
//        pipeline:[
//            { $project:{ _id:0, id:{ $toString:'$_id' }, name:1 } },
//            { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }
//        ]
//    }},
//    { $unwind:'$space' },
//    { $lookup:{
//        from:'task_schedule',
//        as:'user',
//        let:{ id:'$id' },
//        pipeline:[
//            { $project:{ _id:0, taskId:1, assignedTo:1 } },
//            { $match:{ $expr:{ $eq:[ '$taskId', '$$id' ] } } },
//            { $group:{ _id:'$assignedTo'} },
//            { $lookup:{
//                from:'user',
//                as:'user',
//                let:{ id:'$_id' },
//                pipeline:[
//                    { $project:{ _id:0, id:{ $toString:'$_id' }, fullName:1 } },
//                    { $match:{ $expr:{ $eq:[ '$id', '$$id' ] } } }
//                ]
//            }},
//            { $unwind:'$user' },
//            { $replaceRoot:{ newRoot:'$user' } }
//        ]
//    }},
//    { $lookup:{
//        from:'effort_log',
//        as:'effort',
//        let:{ id:'$id' },
//        pipeline:[
//            { $match:{ $expr:{ $eq:['$taskId', '$$id'] } } },
//            { $group:{ _id:'', duration:{ $sum:'$duration' } } }
//        ]
//    } },
//    { $unwind:{ path:'$effort', preserveNullAndEmptyArrays:true } },
//    { $unwind:{ path:'$user', preserveNullAndEmptyArrays:true } },
//    { $group:{
//        _id:{
//            spaceId:'$spaceId',
//            year:{$dateToString: { format: "%Y",  date: "$startDate" }},
//            name:'$space.name',
//            user:'$user.fullName'
//        },
//        duration:{ $sum:'$effort.duration' },
//        assigned:{ $push:'$user' }
//    } },
//    { $project:{ _id:0, space:'$_id.name', year:'$_id.year', assigned:'$_id.user', duration:'$duration'  } },
//    { $sort:{ year:-1, space:1, assigned:1 }  }
}
