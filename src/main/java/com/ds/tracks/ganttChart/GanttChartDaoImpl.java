package com.ds.tracks.ganttChart;

import com.ds.tracks.commons.utils.CollectionName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class GanttChartDaoImpl implements GanttChartDao {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<?> getGanttChartInitialData(String spaceId){

        final String query = "{\n" +
                "        aggregate: '"+CollectionName.spaces+"',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id: 0,\n" +
                "                    title: \"$name\",\n" +
                "                    id: { $toString: \"$_id\" },\n" +
                "                    start: { $toLong: \"$plannedStartDate\" },\n" +
                "                    end: { $toLong: \"$plannedEndDate\" },\n" +
                "                    color: { $ifNull: [\"$color\", \"#c86b98\"] },\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $match: {\n" +
                "                    id: '"+spaceId+"'\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                    from:  '"+CollectionName.sub_spaces+"',\n" +
                "                    let: { spaceId: \"$id\" },\n" +
                "                    pipeline: [{ $match: { $expr: { $eq: [\"$spaceId\", \"$$spaceId\"] } } }, { $project: { _id: 1 } }],\n" +
                "                    as: \"expandable\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    title: \"$title\",\n" +
                "                    id: \"$id\",\n" +
                "                    start: \"$start\",\n" +
                "                    end: \"$end\",\n" +
                "                    color: \"$color\",\n" +
                "                    expandable: {$toBool: { $size: \"$expandable\" }},\n" +
                "                    dType: \"project\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $unionWith: {\n" +
                "                    coll: 'effort_log',\n" +
                "                    pipeline: [\n" +
                "                        {\n" +
                "                            $match: {\n" +
                "                                spaceId:  '"+spaceId+"'\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $project: {\n" +
                "                                _id: 0,\n" +
                "                                id: { $toString: \"$_id\" },\n" +
                "                                logDate: { $toLong: \"$logDate\" }\n" +
                "                            }\n" +
                "\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $sort: {\n" +
                "                                logDate: 1\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $group: {\n" +
                "                                _id: null,\n" +
                "                                id: { $first: \"$id\" },\n" +
                "                                title: { $first: \"Project Time Line\" },\n" +
                "                                start: { $min: \"$logDate\" },\n" +
                "                                end: { $max: \"$logDate\" },\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $project: {\n" +
                "                                _id: 0,\n" +
                "                                id: 1,\n" +
                "                                title: 1,\n" +
                "                                start: 1,\n" +
                "                                end: 1,\n" +
                "                                dType: 'time_line',\n" +
                "                                color: { $ifNull: [\"$color\", \"#c86b98\"] }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            },\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getSubSpaceBySpace(String spaceId){

        final String query = "{\n" +
                "\n" +
                "        aggregate: 'folder',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "                $match: {\n" +
                "                    spaceId: '"+spaceId+"'\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                    from: '"+CollectionName.sub_spaces+"',\n" +
                "                    let: { folderId: '$_id' },\n" +
                "                    pipeline: [\n" +
                "                        {\n" +
                "                            $match: {\n" +
                "                                $expr: { $eq: [\"$folderId\", { $toString: \"$$folderId\" }] }\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $project: { _id: 1 }\n" +
                "                        }\n" +
                "\n" +
                "                    ],\n" +
                "                    as: \"expandable\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id: 0,\n" +
                "                    id: { $toString: \"$_id\" },\n" +
                "                    title: \"$name\",\n" +
                "                    color: { $ifNull: [\"$color\", \"#c86b98\"] },\n" +
                "                    start: { $toLong: \"$plannedStartDate\" },\n" +
                "                    end: { $toLong: \"$plannedEndDate\" },\n" +
                "                    dType: \"folder\",\n" +
                "                    expandable: { $toBool: { $size: \"$expandable\" } }\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $unionWith: {\n" +
                "                    coll: '"+CollectionName.sub_spaces+"',\n" +
                "                    pipeline: [\n" +
                "                        {\n" +
                "                            $match: {\n" +
                "                                spaceId: '"+spaceId+"'\n" +
                "                                folderId: { $in: ['', null] }\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $project: {\n" +
                "                                _id: 0,\n" +
                "                                id: { $toString: \"$_id\" },\n" +
                "                                title: \"$name\",\n" +
                "                                color: { $ifNull: [\"$color\", \"#c86b98\"] },\n" +
                "                                start: { $toLong: \"$plannedStartDate\" },\n" +
                "                                end: { $toLong: \"$plannedEndDate\" },\n" +
                "                                dType: \"sub_space\",\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $lookup: {\n" +
                "                                from: '"+CollectionName.task+"',\n" +
                "                                let: { subSpaceId: \"$id\" },\n" +
                "                                pipeline: [\n" +
                "                                    {\n" +
                "                                        $match: {\n" +
                "                                            $expr: { $eq: [\"$subSpaceId\", \"$$subSpaceId\"] }\n" +
                "                                        }\n" +
                "                                    },\n" +
                "                                    {\n" +
                "                                        $project: { _id: 1 }\n" +
                "                                    }\n" +
                "\n" +
                "\n" +
                "\n" +
                "                                ],\n" +
                "                                as: \"expandable\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            $project: {\n" +
                "                                id: \"$id\",\n" +
                "                                title: \"$title\",\n" +
                "                                color: \"$color\",\n" +
                "                                start: \"$start\",\n" +
                "                                end: \"$end\",\n" +
                "                                dType: \"sub_space\",\n" +
                "                                expandable: { $toBool: { $size: \"$expandable\" } }\n" +
                "                            }\n" +
                "                        },\n" +
                "\n" +
                "                    ]\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getSubSpacesByFolder(String folderId){

        final String query = "{\n" +
                "        aggregate: '"+CollectionName.sub_spaces+"',\n" +
                "        pipeline: [\n" +
                "            { $match:{ folderId:'" +folderId+"' }},\n" +
                "            { $project: {\n" +
                "                    _id: 0,\n" +
                "                    id: { $toString: \"$_id\" },\n" +
                "                    name: '$name',\n" +
                "                    color: '$color',\n" +
                "                    start: { $toLong: '$plannedStartDate' },\n" +
                "                    end: { $toLong: '$plannedEndDate' }\n" +
                "            }},\n" +
                "            { $lookup: {\n" +
                "                from: \"effort_log\",\n" +
                "                let: { subSpaceId: \"$id\" },\n" +
                "                pipeline: [\n" +
                "                  { $match: { $expr: { $eq: [\"$subspaceId\", \"$$subSpaceId\"] } } },\n" +
                "                  { $project: { _id: 0, \"effortDate\": { $toLong: \"$createdAt\" } } },\n" +
                "                ],\n" +
                "                as: \"el\"\n" +
                "            }},\n" +
                "            { $unwind: { path: '$el', preserveNullAndEmptyArrays: true }},\n" +
                "            { $project: {\n" +
                "                id: 1,\n" +
                "                name: 1,\n" +
                "                color: 1,\n" +
                "                start: 1,\n" +
                "                end: 1,\n" +
                "                date: '$el.effortDate'\n" +
                "            }},\n" +
                "            {\n" +
                "                $group: {\n" +
                "                    _id: {\n" +
                "                        id: '$id',\n" +
                "                        name: '$name',\n" +
                "                        color: '$color',\n" +
                "                        start: '$start',\n" +
                "                        end: '$end'\n" +
                "                    },\n" +
                "                    start: { $max: \"$date\" },\n" +
                "                    end: { $min: \"$date\" }\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id:0,\n" +
                "                    id: \"$_id.id\",\n" +
                "                    title: \"$_id.name\",\n" +
                "                    color: \"$_id.color\",\n" +
                "                    start: \"$_id.start\",\n" +
                "                    end: \"$_id.end\",\n" +
                "                    dType:\"sub_space\",\n" +
                "                    expandable:{$toBool: \"true\"}\n" +
                "                }\n" +
                "            }\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getTaskBySubSpace(String subSpaceId){


        final String query = "{\n" +
                "        aggregate: '"+CollectionName.task+"',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "                $match: {\n" +
                "                    subSpaceId: '" +subSpaceId+"'\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id: 0,\n" +
                "                    id: { $toString: \"$_id\" },\n" +
                "                    title: \"$name\",\n" +
                "                    start: { $toLong: \"$startDate\" },\n" +
                "                    end: { $toLong: \"$deadline\" },\n" +
                "                    dType: \"task\",\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                       from: \"sub_tasks\",\n" +
                "                       let: {taskId:\"$id\"},\n" +
                "                       pipeline:[ {$match: {$expr: {$eq: [\"$parentTaskId\",\"$$taskId\"]}}},{$project: {_id:1}} ],\n" +
                "                       as: \"subTasks\"\n" +
                "                     }\n" +
                "            },\n" +
                "                        {\n" +
                "                $project: {\n" +
                "                    id: \"$id\",\n" +
                "                    title: \"$title\",\n" +
                "                    color: \"#c86b98\",\n" +
                "                    start: \"$start\",\n" +
                "                    end: \"$end\",\n" +
                "                    dType: \"task\",\n" +
                "                    expandable: {$toBool: {$size: \"$subTasks\"}},\n" +
                "                }\n" +
                "            },\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
    @Override
    public List<?> getSubTasksByTasks(String taskId){

        final String query = "        {\n" +
                "        aggregate: '"+CollectionName.subtask+"',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "                $match: {\n" +
                "                    parentTaskId: '" +taskId+"'\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id: 0,\n" +
                "                    id: { $toString: \"$_id\" },\n" +
                "                    title: \"$name\",\n" +
                "                    color: \"#c86b98\",\n" +
                "                    start: { $toLong: \"$startDate\" },\n" +
                "                    end: { $toLong: \"$deadline\" },\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getSubSpaceEffortDates(String spaceId){

        final String query = "{\n" +
                "\n" +
                "        aggregate: '"+CollectionName.sub_spaces+"',\n" +
                "        pipeline: [\n" +
                "             {\n" +
                "               $match: {\n" +
                "                   spaceId: '"+spaceId+"'\n" +
                "               }  \n" +
                "             },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id:0,\n" +
                "                    subSpaceId: { $toString: \"$_id\" }\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                    from: '"+CollectionName.folder+"',\n" +
                "                    let: { subSpaceId: \"$subSpaceId\" }\n" +
                "                    pipeline: [{$match:{$expr:{ $in: [\"$$subSpaceId\", \"$subspaces\"] }}},{$project: {_id:0,folderId:{$toString: \"$_id\"}}}],\n" +
                "                    as: \"folder\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $unwind: {path:\"$folder\",preserveNullAndEmptyArrays:true} \n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    subSpaceId:\"$subSpaceId\",\n" +
                "                    folderId:\"$folder.folderId\",\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                      from: '"+CollectionName.effort_log+"',\n" +
                "                      let: { subSpaceId: \"$subSpaceId\" }\n" +
                "                      pipeline: [{$match:{$expr:{ $eq: [\"$$subSpaceId\", \"$subspaceId\"] }}},{$project: {_id:0,logDate:{$toLong: \"$logDate\"}}}],\n" +
                "                      as: \"effort\"\n" +
                "                     }\n" +
                "            },\n" +

                "            {$unwind: {path:\"$effort\",preserveNullAndEmptyArrays:true} },\n" +

                "            {\n" +
                "                $group:{\n" +
                "                    _id:{\n" +
                "                        subSpaceId : \"$subSpaceId\",\n" +
                "                        folderId : \"$folderId\",\n" +
                "                    }\n" +
                "                    start: {$min : \"$effort.logDate\"},\n" +
                "                    end: {$max : \"$effort.logDate\"},\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project:{\n" +
                "                    _id:0,\n" +
                "                    subSpaceId:\"$_id.subSpaceId\",\n" +
                "                    folderId: {$ifNull: [ \"$_id.folderId\", null ]},\n" +
                "                    start:\"$start\",\n" +
                "                    end:\"$end\"\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getTaskEffortDates(String spaceId){

        final String query = "{\n" +
                "        aggregate: '"+CollectionName.task+"',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "              $match: {\n" +
                "                  subSpaceId: '"+spaceId+"'\n" +
                "              }  \n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id:0,\n" +
                "                    taksId: { $toString: \"$_id\" }\n" +
                "                }\n" +
                "            },\n" +
                "\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                      from: 'effort_log',\n" +
                "                      let: { taksId: \"$taksId\" }\n" +
                "                      pipeline: [{$match:{$expr:{ $eq: [\"$$taksId\", \"$taskId\"] }}},{$project: {_id:0,logDate:{$toLong: \"$logDate\"}}}],\n" +
                "                      as: \"effort\"\n" +
                "                     }\n" +
                "            },\n" +
                "            {\n" +
                "                $unwind: {path:\"$effort\",preserveNullAndEmptyArrays:true} \n" +
                "                \n" +
                "            },\n" +
                "            {\n" +
                "                $group:{\n" +
                "                    _id:{\n" +
                "                        taksId : \"$taksId\"\n" +
                "                    }\n" +
                "                    start: {$min : \"$effort.logDate\"},\n" +
                "                    end: {$max : \"$effort.logDate\"},\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project:{\n" +
                "                    _id:0,\n" +
                "                    taksId:\"$_id.taksId\",\n" +
                "                    start:\"$start\",\n" +
                "                    end:\"$end\"\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }

    @Override
    public List<?> getSubTaskEffortDates(String taskId){

        final String query = "{\n" +
                "        aggregate: '"+CollectionName.subtask+"',\n" +
                "        pipeline: [\n" +
                "            {\n" +
                "               $match: {\n" +
                "                   parentTaskId: '"+taskId+"'\n" +
                "               }  \n" +
                "            },\n" +
                "            {\n" +
                "                $project: {\n" +
                "                    _id:0,\n" +
                "                    subTaskId: { $toString: \"$_id\" }\n" +
                "                }\n" +
                "            },\n" +
                "\n" +
                "            {\n" +
                "                $lookup: {\n" +
                "                      from: 'effort_log',\n" +
                "                      let: { subTaskId: \"$subTaskId\" }\n" +
                "                      pipeline: [{$match:{$expr:{ $eq: [\"$$subTaskId\", \"$subTaskId\"] }}},{$project: {_id:0,logDate:{$toLong: \"$logDate\"}}}],\n" +
                "                      as: \"effort\"\n" +
                "                     }\n" +
                "            },\n" +
                "            {\n" +
                "                $unwind: {path:\"$effort\",preserveNullAndEmptyArrays:true} \n" +
                "                \n" +
                "            },\n" +
                "            {\n" +
                "                $group:{\n" +
                "                    _id:{\n" +
                "                        subTaskId : \"$subTaskId\"\n" +
                "                    }\n" +
                "                    start: {$min : \"$effort.logDate\"},\n" +
                "                    end: {$max : \"$effort.logDate\"},\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                $project:{\n" +
                "                    _id:0,\n" +
                "                    subTaskId:\"$_id.subTaskId\",\n" +
                "                    start:\"$start\",\n" +
                "                    end:\"$end\"\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        ], allowDiskUse: true, cursor: { batchSize: 20000000000 }\n" +
                "    }";

        return  (List<Map<Object, Object>>) ((Map) mongoTemplate.executeCommand(query).get("cursor")).get("firstBatch");
    }
}
